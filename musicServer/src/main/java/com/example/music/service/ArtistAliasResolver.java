package com.example.music.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.model.entity.Artist;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ArtistAliasResolver {

    private final ArtistMapper artistMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<Long>> aliasIndex = new ConcurrentHashMap<>();

    @Value("${agent.artist-alias.enabled:true}")
    private boolean enabled;

    @Value("${agent.artist-alias.refresh-on-startup:true}")
    private boolean refreshOnStartup;

    @Value("${agent.artist-alias.redis-key:agent:artist:alias:index:v1}")
    private String redisKey;

    @Value("${agent.artist-alias.redis-ttl-seconds:43200}")
    private long redisTtlSeconds;

    @Value("${agent.artist-alias.override-mappings:}")
    private String overrideMappings;

    public ArtistAliasResolver(ArtistMapper artistMapper, StringRedisTemplate stringRedisTemplate) {
        this.artistMapper = artistMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            return;
        }
        if (loadFromRedis()) {
            return;
        }
        if (refreshOnStartup) {
            rebuildIndex();
        }
    }

    public Set<Long> resolveArtistIds(String raw) {
        if (!enabled || isBlank(raw)) {
            return Collections.emptySet();
        }
        ensureReady();

        Set<Long> ids = new LinkedHashSet<>();
        for (String alias : buildAliasCandidates(raw)) {
            Set<Long> matched = aliasIndex.get(alias);
            if (matched != null && !matched.isEmpty()) {
                ids.addAll(matched);
            }
        }

        if (!ids.isEmpty()) {
            return ids;
        }

        // Final fallback for uncached aliases.
        try {
            List<Long> fuzzyRows = artistMapper.findArtistIdsByKeyword(raw.trim(), 8);
            if (fuzzyRows != null) {
                fuzzyRows.stream().filter(id -> id != null && id > 0).forEach(ids::add);
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    public Map<String, Object> refreshAliasIndex() {
        if (!enabled) {
            return Map.of("enabled", false, "aliasCount", 0, "artistCount", 0);
        }
        Map<String, Set<Long>> rebuilt = rebuildIndex();
        long distinctArtists = rebuilt.values().stream().flatMap(Set::stream).distinct().count();
        return Map.of(
                "enabled", true,
                "aliasCount", rebuilt.size(),
                "artistCount", distinctArtists,
                "redisKey", redisKey
        );
    }

    private void ensureReady() {
        if (!aliasIndex.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (!aliasIndex.isEmpty()) {
                return;
            }
            if (!loadFromRedis()) {
                rebuildIndex();
            }
        }
    }

    private Map<String, Set<Long>> rebuildIndex() {
        Map<String, Set<Long>> rebuilt = new LinkedHashMap<>();
        try {
            List<Artist> artists = artistMapper.selectList(new QueryWrapper<Artist>().select("id", "name"));
            if (artists != null) {
                for (Artist artist : artists) {
                    if (artist == null || artist.getArtistId() == null || isBlank(artist.getArtistName())) {
                        continue;
                    }
                    Long artistId = artist.getArtistId();
                    Set<String> aliases = buildAliasCandidates(artist.getArtistName());
                    for (String alias : aliases) {
                        rebuilt.computeIfAbsent(alias, key -> new LinkedHashSet<>()).add(artistId);
                    }
                }
            }
            applyOverrideMappings(rebuilt);
        } catch (Exception ignored) {
        }

        aliasIndex.clear();
        aliasIndex.putAll(rebuilt);
        saveToRedis(rebuilt);
        return rebuilt;
    }

    private void applyOverrideMappings(Map<String, Set<Long>> target) {
        if (isBlank(overrideMappings)) {
            return;
        }
        List<String> pairs = Arrays.stream(overrideMappings.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty() && item.contains("="))
                .toList();
        for (String pair : pairs) {
            String[] arr = pair.split("=", 2);
            String alias = arr[0].trim();
            String canonical = arr[1].trim();
            if (alias.isEmpty() || canonical.isEmpty()) {
                continue;
            }
            Set<Long> canonicalIds = resolveCanonicalIds(canonical);
            if (canonicalIds.isEmpty()) {
                continue;
            }
            for (String aliasKey : buildAliasCandidates(alias)) {
                target.computeIfAbsent(aliasKey, key -> new LinkedHashSet<>()).addAll(canonicalIds);
            }
        }
    }

    private Set<Long> resolveCanonicalIds(String canonical) {
        Set<Long> ids = new LinkedHashSet<>();
        try {
            List<Long> rows = artistMapper.findArtistIdsByKeyword(canonical, 6);
            if (rows != null) {
                rows.stream().filter(item -> item != null && item > 0).forEach(ids::add);
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    private boolean loadFromRedis() {
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (json == null || json.isEmpty()) {
                return false;
            }
            Map<String, List<Long>> stored = objectMapper.readValue(json, new TypeReference<>() {});
            if (stored == null || stored.isEmpty()) {
                return false;
            }
            aliasIndex.clear();
            for (Map.Entry<String, List<Long>> entry : stored.entrySet()) {
                Set<Long> ids = new LinkedHashSet<>();
                if (entry.getValue() != null) {
                    entry.getValue().stream().filter(item -> item != null && item > 0).forEach(ids::add);
                }
                if (!ids.isEmpty()) {
                    aliasIndex.put(entry.getKey(), ids);
                }
            }
            return !aliasIndex.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void saveToRedis(Map<String, Set<Long>> source) {
        try {
            Map<String, List<Long>> payload = new LinkedHashMap<>();
            for (Map.Entry<String, Set<Long>> entry : source.entrySet()) {
                payload.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            String json = objectMapper.writeValueAsString(payload);
            stringRedisTemplate.opsForValue().set(redisKey, json, redisTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private Set<String> buildAliasCandidates(String raw) {
        if (isBlank(raw)) {
            return Collections.emptySet();
        }
        Set<String> aliases = new LinkedHashSet<>();
        String original = raw.trim();
        aliases.add(normalizeAlias(original));

        String noBracket = original
                .replaceAll("[（(].*?[)）]", " ")
                .replaceAll("[\\[【].*?[\\]】]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        aliases.add(normalizeAlias(noBracket));

        String noPunctuation = original
                .replaceAll("[·•・'`~!@#$%^&*()_+=\\-\\[\\]{};:\"\\|,.<>/?，。！？；：、]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        aliases.add(normalizeAlias(noPunctuation));
        aliases.add(normalizeAlias(noPunctuation.replace(" ", "")));

        String withoutCommonSuffix = original
                .replaceAll("(?i)\\b(band|official|the)\\b", " ")
                .replaceAll("乐队", " ")
                .replaceAll("组合", " ")
                .replaceAll("\\s+", " ")
                .trim();
        aliases.add(normalizeAlias(withoutCommonSuffix));

        aliases.removeIf(String::isEmpty);
        return aliases;
    }

    private String normalizeAlias(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return value;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

