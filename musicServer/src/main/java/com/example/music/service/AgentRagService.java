package com.example.music.service;

import com.example.music.mapper.PlaylistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.StyleMapper;
import com.example.music.model.dto.AgentChatRequestDTO;
import com.example.music.model.dto.RagEvalRequestDTO;
import com.example.music.model.vo.AgentChatResponseVO;
import com.example.music.model.vo.PlaylistVO;
import com.example.music.model.vo.RagEvalVO;
import com.example.music.model.vo.SongVO;
import com.example.music.model.entity.Style;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentRagService {

    private final SongMapper songMapper;
    private final PlaylistMapper playlistMapper;
    private final StyleMapper styleMapper;
    private final SemanticEmbeddingService semanticEmbeddingService;

    @Value("${agent.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${agent.rag.top-k:4}")
    private int ragTopK;

    @Value("${agent.rag.max-snippet-length:120}")
    private int maxSnippetLength;

    @Value("${agent.rag.mode:hybrid}")
    private String ragMode;

    @Value("${agent.rag.keyword-weight:0.45}")
    private double keywordWeight;

    @Value("${agent.rag.vector-weight:0.55}")
    private double vectorWeight;

    @Value("${agent.rag.semantic-weight:0.35}")
    private double semanticWeight;

    @Value("${agent.rag.candidate-multiplier:3}")
    private int candidateMultiplier;

    @Value("${agent.rag.rerank.enabled:true}")
    private boolean rerankEnabled;

    @Value("${agent.rag.rerank.weight:0.25}")
    private double rerankWeight;

    private volatile RetrievalHealth lastRetrievalHealth = RetrievalHealth.idle();

    public AgentRagService(SongMapper songMapper,
                           PlaylistMapper playlistMapper,
                           StyleMapper styleMapper,
                           SemanticEmbeddingService semanticEmbeddingService) {
        this.songMapper = songMapper;
        this.playlistMapper = playlistMapper;
        this.styleMapper = styleMapper;
        this.semanticEmbeddingService = semanticEmbeddingService;
    }

    public RagContext retrieve(String userInput,
                               String intent,
                               AgentChatRequestDTO.NowPlayingDTO nowPlaying,
                               Boolean requestEnableRag,
                               int limit) {
        boolean enabled = requestEnableRag == null ? ragEnabled : requestEnableRag;
        if (!enabled) {
            lastRetrievalHealth = RetrievalHealth.of("disabled", normalizeMode(ragMode), 0, 0, 0, false);
            return RagContext.empty();
        }

        List<String> userQueries = buildUserQueries(userInput);
        List<String> nowPlayingQueries = buildNowPlayingQueries(nowPlaying);
        if (userQueries.isEmpty() && nowPlayingQueries.isEmpty()) {
            lastRetrievalHealth = RetrievalHealth.of("empty_query", normalizeMode(ragMode), 0, 0, 0, true);
            return RagContext.empty();
        }

        int fetchLimit = Math.max(2, Math.min(Math.max(limit, ragTopK), 10));
        RetrievalPack retrievalPack = collectCandidates(userQueries, fetchLimit, "匹配用户问题关键词");
        List<Candidate> candidates = retrievalPack.candidates();
        Set<String> dedupe = retrievalPack.dedupeKeys();

        if (candidates.isEmpty() && shouldFallbackToNowPlaying(userInput, intent) && !nowPlayingQueries.isEmpty()) {
            RetrievalPack fallbackPack = collectCandidates(nowPlayingQueries, fetchLimit, "回退到当前播放上下文");
            candidates.addAll(fallbackPack.candidates());
            dedupe.addAll(fallbackPack.dedupeKeys());
        }

        if (candidates.isEmpty()) {
            int queryCount = userQueries.size() + nowPlayingQueries.size();
            lastRetrievalHealth = RetrievalHealth.of("no_hit", normalizeMode(ragMode), queryCount, 0, 0, true);
            return RagContext.empty();
        }

        String mode = normalizeMode(ragMode);
        String queryForScore = String.join(" ", userQueries.isEmpty() ? nowPlayingQueries : userQueries);
        List<Candidate> preRanked = candidates.stream()
                .map(item -> scoreCandidate(item, queryForScore, mode))
                .sorted(Comparator.comparingDouble(Candidate::totalScore).reversed())
                .limit(Math.max(ragTopK, ragTopK * Math.max(1, candidateMultiplier)))
                .toList();

        List<Candidate> ranked = rerankEnabled
                ? rerankCandidates(preRanked, queryForScore, intent, ragTopK)
                : preRanked.stream().limit(ragTopK).toList();

        List<AgentChatResponseVO.CitationVO> citations = ranked.stream()
                .map(item -> AgentChatResponseVO.CitationVO.builder()
                        .sourceType(item.sourceType())
                        .sourceId(item.sourceId())
                        .title(item.title())
                        .snippet(item.snippet())
                        .reason(item.reason() + "（" + mode + "）")
                        .build())
                .toList();

        lastRetrievalHealth = RetrievalHealth.of(
                mode,
                mode,
                userQueries.size() + nowPlayingQueries.size(),
                candidates.size(),
                citations.size(),
                true
        );

        String promptContext = buildPromptContext(intent, citations);
        return new RagContext(promptContext, citations);
    }

    public RagEvalVO evaluate(RagEvalRequestDTO requestDTO) {
        int topK = Math.max(1, Math.min(requestDTO == null || requestDTO.getTopK() == null ? ragTopK : requestDTO.getTopK(), 20));
        List<RagEvalRequestDTO.CaseDTO> cases = requestDTO == null || requestDTO.getCases() == null ? List.of() : requestDTO.getCases();
        if (cases.isEmpty()) {
            return RagEvalVO.builder()
                    .caseCount(0)
                    .topK(topK)
                    .recallAtK(0D)
                    .mrr(0D)
                    .hitRate(0D)
                    .details(List.of())
                    .build();
        }

        double recallSum = 0D;
        double rrSum = 0D;
        int hitCount = 0;
        List<RagEvalVO.CaseResultVO> details = new ArrayList<>();

        for (RagEvalRequestDTO.CaseDTO testCase : cases) {
            String query = testCase == null ? "" : safe(testCase.getQuery());
            List<AgentChatResponseVO.CitationVO> retrieved = retrieve(query, "CHAT", null, true, topK).citations();
            Set<String> relevantSet = new LinkedHashSet<>();
            if (testCase != null && testCase.getRelevant() != null) {
                for (RagEvalRequestDTO.RelevantDocDTO relevant : testCase.getRelevant()) {
                    if (relevant == null) {
                        continue;
                    }
                    relevantSet.add(buildDocKey(relevant.getSourceType(), relevant.getSourceId()));
                }
            }

            int matched = 0;
            int firstRank = -1;
            for (int i = 0; i < retrieved.size(); i++) {
                AgentChatResponseVO.CitationVO item = retrieved.get(i);
                if (relevantSet.contains(buildDocKey(item.getSourceType(), item.getSourceId()))) {
                    matched++;
                    if (firstRank < 0) {
                        firstRank = i + 1;
                    }
                }
            }

            double recall = relevantSet.isEmpty() ? 0D : ((double) matched / relevantSet.size());
            double rr = firstRank > 0 ? (1D / firstRank) : 0D;
            boolean hit = matched > 0;

            recallSum += recall;
            rrSum += rr;
            if (hit) {
                hitCount++;
            }

            details.add(RagEvalVO.CaseResultVO.builder()
                    .query(query)
                    .hit(hit)
                    .recall(round4(recall))
                    .reciprocalRank(round4(rr))
                    .retrieved(retrieved)
                    .build());
        }

        int n = cases.size();
        return RagEvalVO.builder()
                .caseCount(n)
                .topK(topK)
                .recallAtK(round4(recallSum / n))
                .mrr(round4(rrSum / n))
                .hitRate(round4((double) hitCount / n))
                .details(details)
                .build();
    }

    public RetrievalHealth getLastRetrievalHealth() {
        return lastRetrievalHealth;
    }

    private Candidate scoreCandidate(Candidate item, String queryForScore, String mode) {
        double vectorScore = vectorSimilarity(queryForScore, item.title() + " " + item.snippet());
        double semanticScore = semanticEmbeddingService.semanticSimilarity(queryForScore, item.title() + " " + item.snippet());
        boolean semanticAvailable = semanticEmbeddingService.isConfigured() && semanticScore > 0D;
        double totalScore = switch (mode) {
            case "vector" -> semanticAvailable
                    ? ((0.3D * vectorScore) + (0.7D * semanticScore))
                    : vectorScore;
            case "keyword" -> item.keywordScore();
            default -> {
                double kw = Math.max(0D, keywordWeight);
                double vec = Math.max(0D, vectorWeight);
                double sem = semanticAvailable ? Math.max(0D, semanticWeight) : 0D;
                double totalWeight = kw + vec + sem;
                if (totalWeight <= 0D) {
                    yield (item.keywordScore() + vectorScore + semanticScore) / 3D;
                }
                yield ((kw * item.keywordScore()) + (vec * vectorScore) + (sem * semanticScore)) / totalWeight;
            }
        };
        return new Candidate(
                item.sourceType(),
                item.sourceId(),
                item.title(),
                item.snippet(),
                item.keywordScore(),
                vectorScore,
                totalScore,
                item.reason()
        );
    }

    private double keywordMatchScore(String query, String text, int rankIndex) {
        if (isBlank(text)) {
            return 0;
        }
        String q = safe(query).toLowerCase(Locale.ROOT);
        String t = safe(text).toLowerCase(Locale.ROOT);
        double containBonus = t.contains(q) ? 0.4 : 0.15;
        double rankScore = Math.max(0.1, 1D - (rankIndex * 0.08));
        return Math.min(1D, containBonus + rankScore);
    }

    // Lightweight vector approximation: character bi-gram cosine similarity.
    private double vectorSimilarity(String query, String text) {
        Map<String, Integer> qVec = toGramVector(normalizeForVector(query));
        Map<String, Integer> tVec = toGramVector(normalizeForVector(text));
        if (qVec.isEmpty() || tVec.isEmpty()) {
            return 0;
        }

        double dot = 0;
        for (Map.Entry<String, Integer> entry : qVec.entrySet()) {
            Integer tv = tVec.get(entry.getKey());
            if (tv != null) {
                dot += (double) entry.getValue() * tv;
            }
        }
        double qNorm = Math.sqrt(qVec.values().stream().mapToDouble(v -> v * v).sum());
        double tNorm = Math.sqrt(tVec.values().stream().mapToDouble(v -> v * v).sum());
        if (qNorm == 0 || tNorm == 0) {
            return 0;
        }
        return dot / (qNorm * tNorm);
    }

    private String normalizeForVector(String text) {
        return safe(text)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]", "");
    }

    private Map<String, Integer> toGramVector(String text) {
        Map<String, Integer> vector = new HashMap<>();
        if (isBlank(text)) {
            return vector;
        }
        if (text.length() == 1) {
            vector.put(text, 1);
            return vector;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            String gram = text.substring(i, i + 2);
            vector.merge(gram, 1, Integer::sum);
        }
        return vector;
    }

    private String normalizeMode(String mode) {
        if (isBlank(mode)) {
            return "hybrid";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if ("keyword".equals(normalized) || "vector".equals(normalized)) {
            return normalized;
        }
        return "hybrid";
    }

    private List<String> buildUserQueries(String userInput) {
        Set<String> queries = new LinkedHashSet<>();
        if (!isBlank(userInput)) {
            String normalized = userInput.trim();
            queries.add(normalized);
            String compact = normalized
                    .replaceAll("[，,。！？!?]", " ")
                    .replaceAll("(?i)(请|帮我|推荐|介绍|分析|解析|说说|告诉我|关于|播放|点播|来一首|我要听|帮我放)", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!isBlank(compact) && compact.length() >= 2) {
                queries.add(compact);
            }

            // Extract song keyword from action-style prompts like "播放 Yellow 这首歌".
            String playKeyword = normalized
                    .replaceFirst("^(?i)(播放|点播|来一首|我要听|帮我放)\\s*", "")
                    .replaceAll("(?i)(这首歌|这歌|歌曲)$", "")
                    .trim();
            if (!isBlank(playKeyword) && playKeyword.length() >= 2) {
                queries.add(playKeyword);
            }

            // Try to extract song-like keyword from quoted or bracketed content.
            String quoted = extractQuotedKeyword(normalized);
            if (!isBlank(quoted)) {
                queries.add(quoted);
            }

            queries.addAll(buildSemanticExpansionQueries(normalized));
        }
        return queries.stream().limit(4).toList();
    }

    private List<String> buildSemanticExpansionQueries(String userInput) {
        if (isBlank(userInput)) {
            return List.of();
        }
        String text = userInput.toLowerCase(Locale.ROOT);
        Set<String> expansions = new LinkedHashSet<>();
        if (containsAny(text, "安静", "学习", "专注", "看书")) {
            expansions.add("轻音乐");
            expansions.add("纯音乐");
            expansions.add("钢琴");
            expansions.add("lofi");
        }
        if (containsAny(text, "运动", "健身", "跑步")) {
            expansions.add("动感");
            expansions.add("电子");
            expansions.add("说唱");
        }
        if (containsAny(text, "睡前", "助眠", "放松", "冥想")) {
            expansions.add("治愈");
            expansions.add("轻音乐");
            expansions.add("氛围");
        }
        return new ArrayList<>(expansions);
    }

    private List<Candidate> rerankCandidates(List<Candidate> candidates, String query, String intent, int topK) {
        return candidates.stream()
                .map(item -> {
                    double rerank = rerankScore(query, item, intent);
                    double total = item.totalScore() + rerankWeight * rerank;
                    return new Candidate(
                            item.sourceType(),
                            item.sourceId(),
                            item.title(),
                            item.snippet(),
                            item.keywordScore(),
                            item.vectorScore(),
                            total,
                            item.reason()
                    );
                })
                .sorted(Comparator.comparingDouble(Candidate::totalScore).reversed())
                .limit(topK)
                .toList();
    }

    private double rerankScore(String query, Candidate item, String intent) {
        String content = (safe(item.title()) + " " + safe(item.snippet())).toLowerCase(Locale.ROOT);
        String q = safe(query).toLowerCase(Locale.ROOT);
        double score = 0D;

        if (!isBlank(q) && content.contains(q)) {
            score += 0.45D;
        }

        Set<String> qTerms = toTermSet(q);
        if (!qTerms.isEmpty()) {
            long covered = qTerms.stream().filter(content::contains).count();
            score += Math.min(0.35D, ((double) covered / qTerms.size()) * 0.35D);
        }

        String normalizedIntent = safe(intent).toUpperCase(Locale.ROOT);
        if ("SEARCH_PLAYLIST".equals(normalizedIntent) && "playlist".equalsIgnoreCase(item.sourceType())) {
            score += 0.12D;
        }
        if (("SEARCH_MUSIC".equals(normalizedIntent) || "RECOMMEND".equals(normalizedIntent))
                && "song".equalsIgnoreCase(item.sourceType())) {
            score += 0.12D;
        }

        if (containsAny(q, "学习", "专注", "安静") && containsAny(content, "轻音乐", "纯音乐", "钢琴", "lofi")) {
            score += 0.1D;
        }
        return Math.min(1D, score);
    }

    private Set<String> toTermSet(String text) {
        Set<String> terms = new LinkedHashSet<>();
        if (isBlank(text)) {
            return terms;
        }
        Matcher matcher = Pattern.compile("[\\p{IsIdeographic}A-Za-z0-9]{2,}").matcher(text);
        while (matcher.find()) {
            String token = matcher.group().trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                terms.add(token);
            }
        }
        return terms;
    }

    private List<String> buildNowPlayingQueries(AgentChatRequestDTO.NowPlayingDTO nowPlaying) {
        Set<String> queries = new LinkedHashSet<>();
        if (nowPlaying != null) {
            if (!isBlank(nowPlaying.getTitle())) {
                queries.add(nowPlaying.getTitle().trim());
            }
            if (!isBlank(nowPlaying.getArtist())) {
                queries.add(nowPlaying.getArtist().trim());
            }
        }
        return queries.stream().limit(2).toList();
    }

    private RetrievalPack collectCandidates(List<String> queries, int fetchLimit, String reason) {
        List<Candidate> candidates = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (String query : queries) {
            String styleConstraint = resolveStyleConstraintFromDb(query);
            if (!isBlank(styleConstraint)) {
                List<SongVO> styleSongs = songMapper.searchSongsByStyleKeyword(styleConstraint, fetchLimit);
                if (styleSongs != null) {
                    for (int i = 0; i < styleSongs.size(); i++) {
                        SongVO song = styleSongs.get(i);
                        String key = "song:" + (song.getSongId() == null ? "" : song.getSongId());
                        if (!dedupe.add(key)) {
                            continue;
                        }
                        String title = "歌曲《" + safe(song.getSongName()) + "》- " + safe(song.getArtistName());
                        String snippet = shorten("风格: " + styleConstraint + "，专辑: " + safe(song.getAlbum()) + "，来源: 本地曲库", maxSnippetLength);
                        candidates.add(new Candidate(
                                "song",
                                song.getSongId() == null ? "" : String.valueOf(song.getSongId()),
                                title,
                                snippet,
                                keywordMatchScore(query, title + " " + snippet, i),
                                0D,
                                0D,
                                "匹配风格关键词"
                        ));
                    }
                }
            }

            List<SongVO> songs = songMapper.searchSongsByKeyword(query, fetchLimit);
            if (songs != null) {
                for (int i = 0; i < songs.size(); i++) {
                    SongVO song = songs.get(i);
                    String key = "song:" + (song.getSongId() == null ? "" : song.getSongId());
                    if (!dedupe.add(key)) {
                        continue;
                    }
                    String title = "歌曲《" + safe(song.getSongName()) + "》- " + safe(song.getArtistName());
                    String snippet = shorten("专辑: " + safe(song.getAlbum()) + "，来源: 本地曲库", maxSnippetLength);
                    candidates.add(new Candidate(
                            "song",
                            song.getSongId() == null ? "" : String.valueOf(song.getSongId()),
                            title,
                            snippet,
                            keywordMatchScore(query, title + " " + snippet, i),
                            0D,
                            0D,
                            reason
                    ));
                }
            }

            List<PlaylistVO> playlists = playlistMapper.searchPlaylistsByKeyword(query, fetchLimit);
            if (playlists != null) {
                for (int i = 0; i < playlists.size(); i++) {
                    PlaylistVO playlist = playlists.get(i);
                    String key = "playlist:" + (playlist.getPlaylistId() == null ? "" : playlist.getPlaylistId());
                    if (!dedupe.add(key)) {
                        continue;
                    }
                    String title = "歌单《" + safe(playlist.getTitle()) + "》";
                    String snippet = shorten("来源: 本地歌单库", maxSnippetLength);
                    candidates.add(new Candidate(
                            "playlist",
                            playlist.getPlaylistId() == null ? "" : String.valueOf(playlist.getPlaylistId()),
                            title,
                            snippet,
                            keywordMatchScore(query, title + " " + snippet, i),
                            0D,
                            0D,
                            reason
                    ));
                }
            }
        }
        return new RetrievalPack(candidates, dedupe);
    }

    private String resolveStyleConstraintFromDb(String input) {
        if (isBlank(input)) {
            return "";
        }
        String text = input.toLowerCase(Locale.ROOT);
        try {
            List<Style> styles = styleMapper.selectList(new QueryWrapper<Style>().select("id", "name"));
            if (styles != null) {
                for (Style style : styles) {
                    if (style == null || isBlank(style.getName())) {
                        continue;
                    }
                    String styleName = style.getName().trim();
                    if (text.contains(styleName.toLowerCase(Locale.ROOT))) {
                        return styleName;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean shouldFallbackToNowPlaying(String userInput, String intent) {
        String normalizedIntent = safe(intent).toUpperCase(Locale.ROOT);
        if ("SEARCH_MUSIC".equals(normalizedIntent)
                || "RECOMMEND".equals(normalizedIntent)
                || "CREATE_PLAYLIST".equals(normalizedIntent)
                || "ORGANIZE_PLAYLIST".equals(normalizedIntent)) {
            return false;
        }
        if (isBlank(userInput)) {
            return true;
        }
        String text = userInput.toLowerCase(Locale.ROOT);
        return text.contains("现在播放")
                || text.contains("当前播放")
                || text.contains("这首")
                || text.contains("这歌")
                || text.contains("正在听");
    }

    private boolean containsAny(String text, String... tokens) {
        if (isBlank(text) || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (!isBlank(token) && text.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildDocKey(String sourceType, String sourceId) {
        return safe(sourceType).toLowerCase(Locale.ROOT) + "#" + safe(sourceId);
    }

    private double round4(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    private String extractQuotedKeyword(String text) {
        if (isBlank(text)) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[《\"“](.+?)[》\"”]")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String buildPromptContext(String intent, List<AgentChatResponseVO.CitationVO> citations) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAG资料（按相关性排序）:\n");
        for (int i = 0; i < citations.size(); i++) {
            AgentChatResponseVO.CitationVO item = citations.get(i);
            sb.append(i + 1)
                    .append(". [")
                    .append(item.getSourceType().toUpperCase(Locale.ROOT))
                    .append("] ")
                    .append(safe(item.getTitle()))
                    .append(" | ")
                    .append(safe(item.getSnippet()))
                    .append("\n");
        }
        sb.append("回答约束: 优先依据RAG资料给出回答；若资料不足请明确说明不确定，不要编造事实。\n");
        sb.append("当前意图: ").append(isBlank(intent) ? "CHAT" : intent);
        return sb.toString();
    }

    private String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public record RagContext(String promptContext, List<AgentChatResponseVO.CitationVO> citations) {
        public static RagContext empty() {
            return new RagContext("", List.of());
        }
    }

    private record Candidate(String sourceType,
                             String sourceId,
                             String title,
                             String snippet,
                             double keywordScore,
                             double vectorScore,
                             double totalScore,
                             String reason) {
    }

    private record RetrievalPack(List<Candidate> candidates,
                                 Set<String> dedupeKeys) {
    }

    public record RetrievalHealth(String strategy,
                                  String mode,
                                  int queryCount,
                                  int candidateCount,
                                  int citationCount,
                                  boolean enabled,
                                  long updatedAtEpochMs) {
        public static RetrievalHealth idle() {
            return of("idle", "hybrid", 0, 0, 0, false);
        }

        public static RetrievalHealth of(String strategy,
                                         String mode,
                                         int queryCount,
                                         int candidateCount,
                                         int citationCount,
                                         boolean enabled) {
            return new RetrievalHealth(
                    strategy,
                    mode,
                    queryCount,
                    candidateCount,
                    citationCount,
                    enabled,
                    System.currentTimeMillis()
            );
        }
    }
}

