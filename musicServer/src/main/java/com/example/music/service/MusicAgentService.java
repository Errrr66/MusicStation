package com.example.music.service;

import com.example.music.constant.JwtClaimsConstant;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.PlaylistBindingMapper;
import com.example.music.mapper.PlaylistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.StyleMapper;
import com.example.music.mapper.UserFavoriteMapper;
import com.example.music.model.dto.AgentChatRequestDTO;
import com.example.music.model.dto.AgentPlaylistSaveDTO;
import com.example.music.model.dto.ChatRequestDTO;
import com.example.music.model.entity.Artist;
import com.example.music.model.entity.Playlist;
import com.example.music.model.entity.Style;
import com.example.music.model.entity.UserFavorite;
import com.example.music.model.vo.AgentChatResponseVO;
import com.example.music.model.vo.AgentPlaylistSaveVO;
import com.example.music.model.vo.PlaylistVO;
import com.example.music.model.vo.SongVO;
import com.example.music.result.Result;
import com.example.music.util.ThreadLocalUtil;
import com.example.music.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MusicAgentService {

    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final PlaylistMapper playlistMapper;
    private final PlaylistBindingMapper playlistBindingMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final StyleMapper styleMapper;
    private final ISongService songService;
    private final IPlaylistService playlistService;
    private final AgentRagService agentRagService;
    private final ArtistAliasResolver artistAliasResolver;
    private final DeepSeekService deepSeekService;
    private final SpeechService speechService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${agent.search.connect-timeout-seconds:3}")
    private int searchConnectTimeoutSeconds;

    @Value("${agent.search.request-timeout-seconds:5}")
    private int searchRequestTimeoutSeconds;

    @Value("${agent.search.cache-ttl-seconds:900}")
    private int searchCacheTtlSeconds;

    @Value("${agent.search.circuit-breaker-fail-threshold:3}")
    private int circuitBreakerFailThreshold;

    @Value("${agent.search.circuit-breaker-open-seconds:120}")
    private int circuitBreakerOpenSeconds;

    @Value("${agent.rag.enabled:true}")
    private boolean ragEnabled;

    private static final String AGENT_WEB_SEARCH_CACHE_PREFIX = "agent:web-search:cache:";
    private static final String AGENT_WEB_SEARCH_FAIL_PREFIX = "agent:web-search:fail:";
    private static final String AGENT_WEB_SEARCH_OPEN_PREFIX = "agent:web-search:open:";

    public MusicAgentService(SongMapper songMapper,
                             ArtistMapper artistMapper,
                             PlaylistMapper playlistMapper,
                             PlaylistBindingMapper playlistBindingMapper,
                             UserFavoriteMapper userFavoriteMapper,
                             StyleMapper styleMapper,
                             ISongService songService,
                             IPlaylistService playlistService,
                             AgentRagService agentRagService,
                             ArtistAliasResolver artistAliasResolver,
                             DeepSeekService deepSeekService,
                             SpeechService speechService,
                             StringRedisTemplate stringRedisTemplate) {
        this.songMapper = songMapper;
        this.artistMapper = artistMapper;
        this.playlistMapper = playlistMapper;
        this.playlistBindingMapper = playlistBindingMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.styleMapper = styleMapper;
        this.songService = songService;
        this.playlistService = playlistService;
        this.agentRagService = agentRagService;
        this.artistAliasResolver = artistAliasResolver;
        this.deepSeekService = deepSeekService;
        this.speechService = speechService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public AgentChatResponseVO handle(AgentChatRequestDTO requestDTO, HttpServletRequest request) {
        String userInput = resolveUserInput(requestDTO);
        Intent intent = detectIntent(userInput);
        String playerCommand = resolvePlayerCommand(userInput);
        int limit = Math.max(4, Math.min(requestDTO.getLimit() == null ? 8 : requestDTO.getLimit(), 20));

        List<AgentChatResponseVO.ToolTraceVO> trace = new ArrayList<>();
        List<AgentChatResponseVO.AgentSongCardVO> songs = new ArrayList<>();
        List<AgentChatResponseVO.AgentPlaylistCardVO> playlists = new ArrayList<>();
        List<AgentChatResponseVO.CitationVO> supplementalCitations = new ArrayList<>();
        Map<String, Object> archive = new LinkedHashMap<>();
        String toolSummary;

        switch (intent) {
            case SEARCH_MUSIC -> {
                String searchKeyword = buildSearchKeyword(userInput);
                String singerConstraint = sanitizeSingerKeyword(extractSingerConstraintForSearch(userInput));
                boolean strictSingerMode = !isBlank(singerConstraint) && isExplicitSingerSearch(userInput);

                List<SongVO> localSongRows;
                if (strictSingerMode) {
                    localSongRows = searchSongsBySingerStrict(singerConstraint, Math.max(limit * 6, 80));
                } else {
                    localSongRows = songMapper.searchSongsByKeyword(searchKeyword, Math.max(4, limit / 2));
                    if ((localSongRows == null || localSongRows.isEmpty()) && isPotentialSingerKeyword(searchKeyword)) {
                        List<String> artistCandidates = resolveArtistNameCandidates(searchKeyword, 6);
                        if (!artistCandidates.isEmpty()) {
                            Map<String, SongVO> artistExpandedRows = new LinkedHashMap<>();
                            int artistExpandLimit = Math.max(limit * 4, 40);
                            for (String candidate : artistCandidates) {
                                List<SongVO> rows = songMapper.searchSongsByKeyword(candidate, artistExpandLimit);
                                if (rows == null) {
                                    continue;
                                }
                                for (SongVO row : rows) {
                                    if (row == null) {
                                        continue;
                                    }
                                    String dedupeKey = row.getSongId() == null
                                            ? (safe(row.getSongName()) + "|" + safe(row.getArtistName())).toLowerCase(Locale.ROOT)
                                            : "id:" + row.getSongId();
                                    artistExpandedRows.putIfAbsent(dedupeKey, row);
                                }
                            }
                            localSongRows = new ArrayList<>(artistExpandedRows.values());
                            trace.add(toolOk("artist_keyword_expand", "歌手关键词扩展召回 " + localSongRows.size() + " 首"));
                        }
                    }
                }
                if (strictSingerMode) {
                    localSongRows = filterSongsBySingerStrict(localSongRows, singerConstraint);
                    trace.add(toolOk("search_singer_local_strict", "按歌手严格过滤后 " + localSongRows.size() + " 首: " + singerConstraint));
                }
                List<AgentChatResponseVO.AgentSongCardVO> localSongs = toSongCards(localSongRows, "local", "命中本地曲库", Math.max(4, limit / 2));
                trace.add(toolOk("local_music_search", "本地曲库命中 " + localSongs.size() + " 首"));

                WebSearchResult webResult = !strictSingerMode
                        ? searchWebSongs(searchKeyword, Math.max(4, limit - localSongs.size()))
                        : new WebSearchResult(List.of(), false, false, "指定歌手搜索已启用本地严格模式，未混入外部结果");
                List<AgentChatResponseVO.AgentSongCardVO> webSongs = webResult.songs();
                trace.add(webResult.degraded()
                        ? toolFail("web_music_search", webResult.summary())
                        : toolOk("web_music_search", webResult.summary()));

                songs = mergeSongs(localSongs, webSongs, limit);
                supplementalCitations = buildSongCitationsFromResults(songs);
                toolSummary = "本地曲库和全网搜索均已执行，共返回 " + songs.size() + " 首歌。";
                if (shouldPlaySpecificSong(userInput) && !songs.isEmpty()) {
                    String playTargetKeyword = extractPlayTargetKeyword(userInput);
                    List<AgentChatResponseVO.AgentSongCardVO> rankedSongs = rankSongsForPlayTarget(songs, playTargetKeyword);
                    songs = rankedSongs;
                    double topScore = rankScore(rankedSongs.get(0), playTargetKeyword);
                    if (topScore >= 0.35D) {
                        playerCommand = "play_target";
                    }
                    archive.put("playTargetKeyword", playTargetKeyword);
                    archive.put("playTargetScore", topScore);
                }

                archive.put("searchKeyword", searchKeyword);
                archive.put("localHits", localSongs.size());
                archive.put("webHits", webSongs.size());
                archive.put("webFromCache", webResult.fromCache());
                archive.put("webDegraded", webResult.degraded());
                archive.put("searchSingerStrict", singerConstraint);
                archive.put("strictSingerMode", strictSingerMode);
            }
            case SEARCH_PLAYLIST -> {
                String playlistKeyword = buildPlaylistSearchKeyword(userInput);
                List<PlaylistVO> matched = playlistMapper.searchPlaylistsByKeyword(playlistKeyword, Math.max(5, Math.min(limit, 12)));
                playlists = toPlaylistCards(matched, "local-playlist", "命中本地歌单库", Math.max(5, Math.min(limit, 12)));
                trace.add(toolOk("playlist_search", "本地歌单命中 " + playlists.size() + " 个"));

                if (playlists.isEmpty()) {
                    trace.add(toolFail("playlist_search", "未命中本地歌单，可尝试更具体的歌单标题/风格"));
                    toolSummary = "未命中现有歌单，我可以继续帮你按歌手或风格生成 AI 歌单。";
                } else {
                    toolSummary = "已为你检索到现有歌单结果。";
                }

                supplementalCitations = playlists.stream()
                        .limit(4)
                        .map(item -> AgentChatResponseVO.CitationVO.builder()
                                .sourceType("playlist")
                                .sourceId(item.getPlaylistId() == null ? "" : String.valueOf(item.getPlaylistId()))
                                .title("歌单《" + safe(item.getTitle()) + "》")
                                .snippet("来源: 本地歌单库")
                                .reason("按你的问题命中歌单")
                                .build())
                        .toList();

                archive.put("playlistSearchKeyword", playlistKeyword);
                archive.put("playlistHits", playlists.size());
                archive.put("intentDecision", "search_playlist");
            }
            case RECOMMEND -> {
                Result<List<SongVO>> songResult = songService.getRecommendedSongs(request);
                Result<List<PlaylistVO>> playlistResult = playlistService.getRecommendedPlaylists(request);
                String singerConstraint = sanitizeSingerKeyword(extractSingerConstraintForRecommend(userInput));
                String styleConstraint = resolveStyleConstraint(userInput);
                String preferKeyword = extractRecommendKeyword(userInput);

                // 1) 歌手需求：严格歌手模式，绝不混入其他歌手，也不参考当前播放
                if (!isBlank(singerConstraint)) {
                    List<SongVO> strictSingerSongs = searchSongsBySingerStrict(singerConstraint, Math.max(limit * 6, 80));

                    songs = toSongCards(strictSingerSongs, "local", "严格按指定歌手本地曲库推荐", limit);
                    trace.add(toolOk("recommend_singer_local_strict", "按歌手严格命中本地曲库 " + songs.size() + " 首: " + singerConstraint));

                    AgentChatResponseVO.AgentPlaylistCardVO singerPlaylist = AgentChatResponseVO.AgentPlaylistCardVO.builder()
                            .playlistId(null)
                            .title("AI歌单 · " + singerConstraint)
                            .coverUrl(songs.isEmpty() ? null : songs.get(0).getCoverUrl())
                            .source("ai-agent")
                            .reason(songs.isEmpty() ? "本地曲库暂未命中该歌手歌曲" : "严格按指定歌手整合本地歌曲")
                            .songCount(songs.size())
                            .tracks(songs)
                            .build();
                    playlists = List.of(singerPlaylist);

                    trace.add(toolOk("smart_recommendation", "已按指定歌手完成本地严格推荐，歌曲 " + songs.size() + " 首"));
                    toolSummary = "已按你指定的歌手严格整合本地曲库推荐，未混入其他歌手。";
                    archive.put("recommendSongCount", songs.size());
                    archive.put("recommendPlaylistCount", playlists.size());
                    archive.put("recommendKeyword", singerConstraint);
                    archive.put("recommendMode", "singer_local_strict");
                    break;
                }

                // 2) 风格需求：真实按风格推荐，不参考当前播放
                if (!isBlank(styleConstraint)) {
                    List<AgentChatResponseVO.AgentSongCardVO> localSongs = toSongCards(
                            songMapper.searchSongsByStyleKeyword(styleConstraint, Math.max(limit * 3, 30)),
                            "local",
                            "按风格命中本地曲库",
                            limit
                    );

                    if (localSongs.size() < limit) {
                        WebSearchResult webResult = searchWebSongs(styleConstraint, limit - localSongs.size());
                        songs = mergeSongs(localSongs, webResult.songs(), limit);
                        trace.add(webResult.degraded()
                                ? toolFail("recommend_style_web_fill", webResult.summary())
                                : toolOk("recommend_style_web_fill", "按风格补全外网 " + webResult.songs().size() + " 首"));
                    } else {
                        songs = localSongs;
                    }

                    AgentChatResponseVO.AgentPlaylistCardVO stylePlaylist = AgentChatResponseVO.AgentPlaylistCardVO.builder()
                            .playlistId(null)
                            .title("AI歌单 · " + styleConstraint)
                            .coverUrl(songs.isEmpty() ? null : songs.get(0).getCoverUrl())
                            .source("ai-agent")
                            .reason("按风格推荐（不基于当前播放）")
                            .songCount(songs.size())
                            .tracks(songs)
                            .build();
                    playlists = List.of(stylePlaylist);

                    trace.add(toolOk("smart_recommendation", "已按风格推荐，歌曲 " + songs.size() + " 首"));
                    toolSummary = "已按风格真实推荐，未参考当前播放歌曲。";
                    archive.put("recommendSongCount", songs.size());
                    archive.put("recommendPlaylistCount", playlists.size());
                    archive.put("recommendKeyword", styleConstraint);
                    archive.put("recommendMode", "style");
                    break;
                }

                // 3) 普通推荐：关键词本地优先 + 个性化兜底 + 外网补全，不注入当前播放歌单
                List<AgentChatResponseVO.AgentSongCardVO> localSongs;
                if (!isBlank(preferKeyword)) {
                    localSongs = searchLocalSongs(preferKeyword, limit);
                    trace.add(toolOk("recommend_local_keyword", "按关键词命中本地曲库 " + localSongs.size() + " 首: " + preferKeyword));
                } else {
                    localSongs = toSongCards(songResult.getData(), "recommend", "基于你的收藏偏好推荐", limit);
                }

                if (localSongs.isEmpty()) {
                    localSongs = toSongCards(songResult.getData(), "recommend", "基于你的收藏偏好推荐", limit);
                }

                if (localSongs.size() < limit) {
                    String recommendKeyword = !isBlank(preferKeyword) ? preferKeyword : "popular";
                    WebSearchResult webResult = searchWebSongs(recommendKeyword, limit - localSongs.size());
                    songs = mergeSongs(localSongs, webResult.songs(), limit);
                    trace.add(webResult.degraded()
                            ? toolFail("recommend_web_fill", webResult.summary())
                            : toolOk("recommend_web_fill", webResult.summary()));
                    archive.put("recommendLocalHits", localSongs.size());
                    archive.put("recommendWebHits", webResult.songs().size());
                    archive.put("recommendWebFromCache", webResult.fromCache());
                } else {
                    songs = localSongs;
                }

                playlists = toPlaylistCards(playlistResult.getData(), "recommend", "与你近期偏好相近", 5);
                trace.add(toolOk("smart_recommendation", "推荐歌曲 " + songs.size() + " 首，推荐歌单 " + playlists.size() + " 个"));
                toolSummary = "推荐引擎已返回歌曲与歌单结果。";
                archive.put("recommendSongCount", songs.size());
                archive.put("recommendPlaylistCount", playlists.size());
                archive.put("recommendKeyword", preferKeyword);
                archive.put("recommendMode", "general");
            }
            case ANALYZE_NOW_PLAYING -> {
                AgentChatRequestDTO.NowPlayingDTO nowPlaying = requestDTO.getNowPlaying();
                if (nowPlaying == null || isBlank(nowPlaying.getTitle())) {
                    trace.add(toolFail("now_playing_analyzer", "未检测到当前播放歌曲"));
                    toolSummary = "当前没有可分析的播放上下文。";
                } else {
                    songs = searchLocalSongs(nowPlaying.getArtist(), 4);
                    trace.add(toolOk("now_playing_analyzer", "正在播放: " + nowPlaying.getTitle() + " - " + nowPlaying.getArtist()));
                    trace.add(toolOk("similar_song_search", "同风格/同歌手候选 " + songs.size() + " 首"));
                    toolSummary = "已分析正在播放歌曲并给出相似歌曲建议。";
                    archive.put("nowPlaying", nowPlaying);
                    archive.put("similarCount", songs.size());
                }
            }
            case CREATE_PLAYLIST -> {
                String strictArtist = extractArtistConstraint(userInput);
                String strictStyle = resolveStyleConstraint(userInput);
                boolean strictPlaylistIntent = !isBlank(strictArtist) || !isBlank(strictStyle);
                toolSummary = "已按你的描述生成歌单草案。";

                songs = buildPlaylistByPrompt(userInput, limit);
                if (songs.isEmpty()) {
                    if (strictPlaylistIntent) {
                        String missReason = !isBlank(strictArtist)
                                ? "本地曲库未命中指定歌手歌曲: " + strictArtist
                                : "本地曲库未命中指定风格歌曲: " + strictStyle;
                        trace.add(toolFail("playlist_auto_builder", missReason));
                        toolSummary = missReason + "，未引入无关歌曲。";
                        archive.put("playlistStrictIntent", true);
                        archive.put("playlistStrictArtist", strictArtist);
                        archive.put("playlistStrictStyle", strictStyle);
                    } else {
                        // 泛化意图才允许随机回退，避免返回空歌单。
                        songs = toSongCards(songMapper.getRandomSongsWithArtist(), "local", "本地曲库回退推荐", limit);
                    }
                }
                AgentChatResponseVO.AgentPlaylistCardVO generated = AgentChatResponseVO.AgentPlaylistCardVO.builder()
                        .playlistId(null)
                        .title("AI歌单: " + shorten(userInput))
                        .coverUrl(songs.isEmpty() ? null : songs.get(0).getCoverUrl())
                        .source("ai-agent")
                        .reason(songs.isEmpty() && strictPlaylistIntent
                                ? "未命中指定歌手/风格，本次未引入无关歌曲"
                                : "根据你的意图自动生成，可一键预览播放")
                        .songCount(songs.size())
                        .tracks(songs)
                        .build();
                playlists = List.of(generated);
                if (!songs.isEmpty()) {
                    trace.add(toolOk("playlist_auto_builder", "已自动生成歌单草案，包含 " + songs.size() + " 首歌"));
                    toolSummary = "已按你的描述生成歌单草案。";
                }
                archive.put("playlistDraftTitle", generated.getTitle());
                archive.put("draftSongCount", songs.size());
            }
            case ORGANIZE_PLAYLIST -> {
                songs = organizePlaylistSeeds(requestDTO.getPlaylistSeeds(), limit);
                trace.add(toolOk("playlist_organizer", "已完成歌单整理，输出 " + songs.size() + " 首优先项"));
                toolSummary = "已根据歌单条目完成分组整理，建议先试听前几首。";
                archive.put("organizedCount", songs.size());
            }
            case PLAYER_CONTROL -> {
                trace.add(toolOk("voice_player_control", "识别为播放器控制指令: " + playerCommand));
                toolSummary = "已识别播放器控制命令。";
            }
            default -> {
                trace.add(toolOk("agent_chat", "已使用通用对话模式"));
                toolSummary = "保持通用对话并准备调用工具。";
            }
        }

        String answer;
        String japanese = "";
        AgentRagService.RagContext ragContext = AgentRagService.RagContext.empty();
        List<AgentChatResponseVO.CitationVO> mergedCitations;

        if (intent != Intent.PLAYER_CONTROL) {
            boolean requestEnableRag = requestDTO.getEnableRag() == null ? ragEnabled : requestDTO.getEnableRag();
            boolean useRag = requestEnableRag && shouldUseRagForInput(userInput, intent);
            if (useRag) {
                ragContext = agentRagService.retrieve(userInput, intent.name(), requestDTO.getNowPlaying(), true, limit);
                if (!ragContext.citations().isEmpty()) {
                    trace.add(toolOk("rag_retriever", "知识检索命中 " + ragContext.citations().size() + " 条"));
                } else {
                    trace.add(toolOk("rag_retriever", "知识检索未命中，回退常规回答"));
                }
            } else {
                ragContext = AgentRagService.RagContext.empty();
                trace.add(toolOk("rag_retriever", "常识/通用对话已直答，未启用RAG检索"));
            }
            archive.put("ragEnabled", useRag);
            archive.put("ragCitationCount", ragContext.citations().size());
            AgentRagService.RetrievalHealth retrievalHealth = agentRagService.getLastRetrievalHealth();
            archive.put("ragStrategy", retrievalHealth.strategy());
            archive.put("ragMode", retrievalHealth.mode());
        }

        mergedCitations = mergeCitations(ragContext.citations(), supplementalCitations, Math.min(limit, 8));
        if (!supplementalCitations.isEmpty()) {
            archive.put("supplementalCitationCount", supplementalCitations.size());
        }

        if (intent == Intent.PLAYER_CONTROL) {
            answer = switch (playerCommand) {
                case "play" -> "已收到，继续播放。";
                case "pause" -> "好的，已为你暂停播放。";
                case "next" -> "收到，切到下一首。";
                case "prev" -> "没问题，返回上一首。";
                default -> "我听到你的控制指令了，你可以说“播放/暂停/下一首/上一首”。";
            };
        } else {
            TextPair pair = buildAgentNarration(userInput, intent.name(), toolSummary, requestDTO.getNowPlaying(), ragContext.promptContext());
            answer = pair.chinese;
            japanese = pair.japanese;
        }

        String audioUrl = "";
        boolean enableVoice = requestDTO.getEnableVoice() == null || requestDTO.getEnableVoice();
        if (enableVoice && !isBlank(japanese)) {
            String cleaned = japanese.replaceAll("(?s)（.*?）", "").replaceAll("(?s)\\(.*?\\)", "").trim();
            if (!cleaned.isEmpty()) {
                audioUrl = speechService.generateAndUploadSpeech(cleaned, 6);
            }
        }

        archive.put("intent", intent.name());
        archive.put("playerCommand", playerCommand);
        archive.put("generatedAt", LocalDateTime.now().toString());
        archive.put("userId", resolveUserId());

        return AgentChatResponseVO.builder()
                .answer(answer)
                .audio(audioUrl == null ? "" : audioUrl)
                .intent(intent.name())
                .playerCommand(playerCommand)
                .toolTrace(trace)
                .songs(songs)
                .playlists(playlists)
                .citations(mergedCitations)
                .musicArchive(archive)
                .build();
    }

    private List<AgentChatResponseVO.CitationVO> buildSongCitationsFromResults(List<AgentChatResponseVO.AgentSongCardVO> resultSongs) {
        final int limit = 4;
        if (resultSongs == null || resultSongs.isEmpty()) {
            return List.of();
        }

        List<AgentChatResponseVO.CitationVO> citations = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (AgentChatResponseVO.AgentSongCardVO song : resultSongs) {
            if (citations.size() >= limit) {
                break;
            }
            if (song == null || isBlank(song.getSongName())) {
                continue;
            }
            String dedupeKey = (safe(song.getSongName()) + "|" + safe(song.getArtistName())).toLowerCase(Locale.ROOT);
            if (!dedupe.add(dedupeKey)) {
                continue;
            }

            String sourceType = "song";
            if (!isBlank(song.getSource()) && !"local".equalsIgnoreCase(song.getSource())) {
                sourceType = "web-song";
            }
            String sourceId = safe(song.getSongId() == null ? "" : String.valueOf(song.getSongId()));
            if (isBlank(sourceId)) {
                sourceId = dedupeKey;
            }

            String title = "歌曲《" + safe(song.getSongName()) + "》- " + safe(song.getArtistName());
            String snippet = "专辑: " + safe(song.getAlbum()) + "，来源: " + safe(song.getSource());
            citations.add(AgentChatResponseVO.CitationVO.builder()
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .title(title)
                    .snippet(snippet)
                    .reason("按你的问题命中歌曲结果")
                    .build());
        }
        return citations;
    }

    private List<AgentChatResponseVO.CitationVO> mergeCitations(List<AgentChatResponseVO.CitationVO> primary,
                                                                List<AgentChatResponseVO.CitationVO> secondary,
                                                                int limit) {
        List<AgentChatResponseVO.CitationVO> merged = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();

        if (primary != null) {
            for (AgentChatResponseVO.CitationVO item : primary) {
                if (item == null) {
                    continue;
                }
                String key = (safe(item.getSourceType()) + "|" + safe(item.getSourceId()) + "|" + safe(item.getTitle())).toLowerCase(Locale.ROOT);
                if (dedupe.add(key)) {
                    merged.add(item);
                }
                if (merged.size() >= limit) {
                    return merged;
                }
            }
        }

        if (secondary != null) {
            for (AgentChatResponseVO.CitationVO item : secondary) {
                if (item == null) {
                    continue;
                }
                String key = (safe(item.getSourceType()) + "|" + safe(item.getSourceId()) + "|" + safe(item.getTitle())).toLowerCase(Locale.ROOT);
                if (dedupe.add(key)) {
                    merged.add(item);
                }
                if (merged.size() >= limit) {
                    break;
                }
            }
        }
        return merged;
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    public AgentPlaylistSaveVO saveAgentPlaylist(AgentPlaylistSaveDTO saveDTO, HttpServletRequest request) {
        Long userId = resolveUserIdFromRequest(request);
        if (userId == null) {
            throw new RuntimeException("请先登录后再保存歌单");
        }
        if (saveDTO == null || saveDTO.getTracks() == null || saveDTO.getTracks().isEmpty()) {
            throw new RuntimeException("歌单内容为空，无法保存");
        }

        List<Long> songIds = saveDTO.getTracks().stream()
                .map(AgentPlaylistSaveDTO.TrackDTO::getSongId)
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
        if (songIds.isEmpty()) {
            throw new RuntimeException("未找到可落库的歌曲 ID");
        }

        String safeTitle = isBlank(saveDTO.getTitle()) ? "AI歌单" : saveDTO.getTitle().trim();
        String finalTitle = buildUniquePlaylistTitle("[AI] " + safeTitle);
        Playlist playlist = new Playlist();
        playlist.setTitle(finalTitle);
        playlist.setIntroduction(isBlank(saveDTO.getIntroduction()) ? "由 AI 助手自动生成" : saveDTO.getIntroduction().trim());
        playlist.setStyle(isBlank(saveDTO.getStyle()) ? "AI推荐" : saveDTO.getStyle().trim());
        playlist.setCoverUrl(saveDTO.getCoverUrl());
        playlistMapper.insert(playlist);

        int inserted = playlistBindingMapper.batchInsertBindings(playlist.getPlaylistId(), songIds);
        autoCollectPlaylist(userId, playlist.getPlaylistId());

        return AgentPlaylistSaveVO.builder()
                .playlistId(playlist.getPlaylistId())
                .title(playlist.getTitle())
                .songCount(inserted)
                .build();
    }

    private String resolveUserInput(AgentChatRequestDTO requestDTO) {
        if (requestDTO == null) {
            return "";
        }
        if (!isBlank(requestDTO.getMessage())) {
            return requestDTO.getMessage().trim();
        }
        if (requestDTO.getMessages() != null && !requestDTO.getMessages().isEmpty()) {
            for (int i = requestDTO.getMessages().size() - 1; i >= 0; i--) {
                AgentChatRequestDTO.ChatMessageDTO item = requestDTO.getMessages().get(i);
                if (item != null && "user".equalsIgnoreCase(item.getRole()) && !isBlank(item.getContent())) {
                    return item.getContent().trim();
                }
            }
        }
        return "";
    }

    private Intent detectIntent(String input) {
        String text = input == null ? "" : input.toLowerCase(Locale.ROOT);

        if (isLikelyKnowledgeQuestion(input)) {
            return Intent.CHAT;
        }

        if (isPlaylistSearchIntent(input)) {
            return Intent.SEARCH_PLAYLIST;
        }
        if (isCreatePlaylistIntent(input)) {
            return Intent.CREATE_PLAYLIST;
        }
        if (containsAny(text, "下一首", "上一首", "暂停", "skip", "pause", "previous", "prev", "next")) {
            return Intent.PLAYER_CONTROL;
        }
        if (isPurePlayCommand(text)) {
            return Intent.PLAYER_CONTROL;
        }
        if (shouldPlaySpecificSong(text) || containsAny(text, "点播", "来一首", "我要听")) {
            return Intent.SEARCH_MUSIC;
        }
        if (containsAny(text, "整理歌单", "归档", "清理歌单", "按风格整理")) {
            return Intent.ORGANIZE_PLAYLIST;
        }
        if (containsAny(text, "推荐", "听什么", "猜你喜欢", "随机来点")) {
            return Intent.RECOMMEND;
        }
        if (containsAny(text, "生成歌单", "做个歌单", "制作歌单", "创建歌单")) {
            return Intent.CREATE_PLAYLIST;
        }
        if (containsAny(text, "正在播放", "这首歌", "解析", "分析歌曲", "介绍这首")) {
            return Intent.ANALYZE_NOW_PLAYING;
        }
        if (containsAny(text, "搜索", "找歌", "全网", "web", "net")) {
            return Intent.SEARCH_MUSIC;
        }
        if (isLikelyDirectMusicSearch(input)) {
            return Intent.SEARCH_MUSIC;
        }
        return Intent.CHAT;
    }

    private boolean isCreatePlaylistIntent(String input) {
        if (isBlank(input)) {
            return false;
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (!text.contains("歌单") && !text.contains("playlist")) {
            return false;
        }
        return containsAny(text,
                "生成", "制作", "创建", "新建", "做个", "做一个", "做一份", "做一张",
                "来个", "来一份", "来一张", "给我", "帮我", "整一份", "配一份", "定制", "帮我做");
    }


    private String resolvePlayerCommand(String input) {
        if (isBlank(input)) {
            return "none";
        }
        String text = input.toLowerCase(Locale.ROOT);
        if (shouldPlaySpecificSong(text)) {
            return "play_target";
        }
        if (containsAny(text, "下一首", "next", "skip")) {
            return "next";
        }
        if (containsAny(text, "上一首", "prev", "previous")) {
            return "prev";
        }
        if (containsAny(text, "暂停", "pause", "停止")) {
            return "pause";
        }
        if (containsAny(text, "播放", "继续", "resume", "play")) {
            return "play";
        }
        return "none";
    }

    private String buildSearchKeyword(String input) {
        if (isBlank(input)) {
            return "";
        }
        String keyword = input.trim();
        String[] prefixes = {"播放", "点播", "来一首", "我要听", "帮我放", "来点"};
        for (String prefix : prefixes) {
            if (keyword.startsWith(prefix)) {
                keyword = keyword.substring(prefix.length()).trim();
                break;
            }
        }
        keyword = keyword
                .replaceAll("(?i)^歌手\\s*", "")
                .replaceAll("(?i)^(有没有|有吗|还有吗|是否有|能不能|可不可以|可以吗|有没有人唱)\\s*", "")
                .replaceAll("(?i)(这首歌|这歌|歌曲)$", "")
                .replaceAll("(?i)(的?歌|的?歌曲|的?音乐|歌单|单曲)$", "")
                .replaceAll("(?i)(吧|好吗|行吗)$", "")
                .replaceAll("(?i)(给我|帮我|请|麻烦)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return keyword.isEmpty() ? input.trim() : keyword;
    }

    private String extractSingerConstraintForSearch(String input) {
        if (isBlank(input)) {
            return "";
        }
        String text = input.trim();

        Matcher m1 = Pattern.compile("(?:播放|点播|来一首|我要听|帮我放)\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,40})\\s*的\\s*.+").matcher(text);
        if (m1.find()) {
            String candidate = sanitizeSingerKeyword(m1.group(1));
            return isGenericPlaylistWord(candidate) ? "" : candidate;
        }

        Matcher m2 = Pattern.compile("歌手\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,40})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m2.find()) {
            String candidate = sanitizeSingerKeyword(m2.group(1));
            return isGenericPlaylistWord(candidate) ? "" : candidate;
        }

        Matcher m3 = Pattern.compile("^([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,40})\\s*的\\s*(歌|歌曲|音乐)$", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m3.find()) {
            String candidate = sanitizeSingerKeyword(m3.group(1));
            return isGenericPlaylistWord(candidate) ? "" : candidate;
        }
        return "";
    }

    private boolean isPurePlayCommand(String text) {
        if (isBlank(text)) {
            return false;
        }
        String normalized = text.trim();
        return normalized.equals("播放")
                || normalized.equals("继续")
                || normalized.equals("继续播放")
                || normalized.equals("resume")
                || normalized.equals("play");
    }

    private boolean shouldPlaySpecificSong(String text) {
        if (isBlank(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("播放") && normalized.length() > 2
                || normalized.startsWith("play ")
                || normalized.startsWith("点播")
                || normalized.startsWith("来一首")
                || normalized.startsWith("我要听")
                || normalized.startsWith("帮我放");
    }

    private List<AgentChatResponseVO.AgentSongCardVO> searchLocalSongs(String keyword, int limit) {
        if (isBlank(keyword)) {
            return List.of();
        }
        List<SongVO> list = songMapper.searchSongsByKeyword(keyword, limit);
        return toSongCards(list, "local", "命中本地曲库", limit);
    }

    private WebSearchResult searchWebSongs(String keyword, int limit) {
        if (isBlank(keyword) || limit <= 0) {
            return new WebSearchResult(List.of(), false, false, "全网搜索参数无效，已跳过");
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        String cacheKey = AGENT_WEB_SEARCH_CACHE_PREFIX + normalizedKeyword + ":" + limit;

        List<AgentChatResponseVO.AgentSongCardVO> cached = getWebSearchCache(cacheKey);
        if (!cached.isEmpty()) {
            return new WebSearchResult(cached, false, true, "全网搜索命中缓存 " + cached.size() + " 首");
        }

        if (isCircuitBreakerOpen(normalizedKeyword)) {
            List<AgentChatResponseVO.AgentSongCardVO> fallback = searchLocalSongs(keyword, limit);
            if (fallback.isEmpty()) {
                fallback = toSongCards(songMapper.getRandomSongsWithArtist(), "fallback", "外部搜索熔断，回退本地随机推荐", limit);
            }
            return new WebSearchResult(fallback, true, false, "外部搜索熔断中，已回退本地结果 " + fallback.size() + " 首");
        }

        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://itunes.apple.com/search?entity=song&limit=" + limit + "&term=" + encoded;

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(searchConnectTimeoutSeconds))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(searchRequestTimeoutSeconds))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return degradeWebSearch(keyword, limit, normalizedKeyword, "外部搜索状态码异常: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return degradeWebSearch(keyword, limit, normalizedKeyword, "外部搜索未返回有效数据");
            }

            List<AgentChatResponseVO.AgentSongCardVO> cards = new ArrayList<>();
            for (JsonNode item : results) {
                if (cards.size() >= limit) {
                    break;
                }
                cards.add(AgentChatResponseVO.AgentSongCardVO.builder()
                        .songId(null)
                        .songName(item.path("trackName").asText(""))
                        .artistName(item.path("artistName").asText(""))
                        .album(item.path("collectionName").asText(""))
                        .coverUrl(item.path("artworkUrl100").asText(""))
                        .audioUrl(item.path("previewUrl").asText(""))
                        .source("itunes")
                        .reason("全网搜索结果")
                        .build());
            }
            if (!cards.isEmpty()) {
                resetCircuitBreaker(normalizedKeyword);
                setWebSearchCache(cacheKey, cards);
            }
            return new WebSearchResult(cards, false, false, "全网搜索命中 " + cards.size() + " 首");
        } catch (Exception e) {
            return degradeWebSearch(keyword, limit, normalizedKeyword, "外部搜索失败，已回退本地结果");
        }
    }

    private WebSearchResult degradeWebSearch(String keyword, int limit, String normalizedKeyword, String reason) {
        triggerCircuitBreaker(normalizedKeyword);
        List<AgentChatResponseVO.AgentSongCardVO> fallback = searchLocalSongs(keyword, limit);
        if (fallback.isEmpty()) {
            fallback = toSongCards(songMapper.getRandomSongsWithArtist(), "fallback", "外部搜索不可用，回退本地随机推荐", limit);
        }
        return new WebSearchResult(fallback, true, false, reason + "，回退 " + fallback.size() + " 首");
    }

    private List<AgentChatResponseVO.AgentSongCardVO> buildPlaylistByPrompt(String prompt, int limit) {
        String artistConstraint = extractArtistConstraint(prompt);
        String styleConstraint = resolveStyleConstraint(prompt);

        if (!isBlank(artistConstraint)) {
            List<SongVO> artistSongs = searchSongsBySingerStrict(artistConstraint, Math.max(limit * 6, 80));

            // 指定歌手模式禁止混入其他歌手：命中就返回歌手曲目，未命中则返回空
            return toSongCards(artistSongs, "local", "按指定歌手生成歌单", limit);
        }

        if (!isBlank(styleConstraint)) {
            List<SongVO> styleSongs = songMapper.searchSongsByStyleKeyword(styleConstraint, Math.max(limit * 3, 30));
            // 指定风格模式禁止混入无关风格：命中就返回风格曲目，未命中则返回空
            return toSongCards(styleSongs, "local", "按指定风格生成歌单", limit);
        }

        List<SongVO> candidates;
        candidates = songMapper.searchSongsByKeyword(prompt, limit);

        // 口语请求如“推荐Coldplay的歌单”优先再用清洗后的关键词兜底一次
        if ((candidates == null || candidates.isEmpty()) && !isBlank(prompt)) {
            String fallbackKeyword = extractRecommendKeyword(prompt);
            if (!isBlank(fallbackKeyword) && !fallbackKeyword.equalsIgnoreCase(prompt.trim())) {
                candidates = songMapper.searchSongsByKeyword(fallbackKeyword, limit);
            }
        }

        if (candidates == null || candidates.isEmpty()) {
            candidates = songMapper.getRandomSongsWithArtist().stream().limit(limit).toList();
        }

        return toSongCards(candidates, "local", "适配你的歌单意图", limit);
    }

    private String extractRecommendKeyword(String input) {
        if (isBlank(input)) {
            return "";
        }
        String keyword = input.trim();

        // 常见口语清洗：推荐周杰伦的歌 -> 周杰伦
        keyword = keyword
                .replaceAll("[，,。！？!?]", " ")
                .replaceAll("(?i)(给我|帮我|请|麻烦|推荐|来点|来一些|来一首|想听|听点)", " ")
                .replaceAll("(?i)(歌曲|歌单|音乐|歌曲儿|歌)", " ")
                .replaceAll("的", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // 过短关键词基本无法稳定命中，交给默认推荐
        if (keyword.length() < 2) {
            return "";
        }
        return keyword;
    }

    private String extractSingerConstraintForRecommend(String input) {
        if (isBlank(input)) {
            return "";
        }
        String text = input.trim();

        Matcher m1 = Pattern.compile("推荐\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,40})\\s*的?\\s*歌曲?").matcher(text);
        if (m1.find()) {
            String candidate = sanitizeSingerKeyword(m1.group(1));
            return isGenericPlaylistWord(candidate) ? "" : candidate;
        }

        Matcher m2 = Pattern.compile("歌手\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,40})").matcher(text);
        if (m2.find()) {
            String candidate = sanitizeSingerKeyword(m2.group(1));
            return isGenericPlaylistWord(candidate) ? "" : candidate;
        }

        return "";
    }

    private String resolveStyleConstraint(String input) {
        if (isBlank(input)) {
            return "";
        }
        String text = input.toLowerCase(Locale.ROOT);
        List<String> styles = loadKnownStyleNames();

        Matcher styleMatcher = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9&\\-]{2,20})\\s*风格").matcher(input);
        if (styleMatcher.find()) {
            String rawStyle = styleMatcher.group(1).trim();
            String matched = matchKnownStyle(rawStyle, styles);
            if (!isBlank(matched)) {
                return matched;
            }
        }

        if (containsAny(text, "r&b", "rb", "rnb")) {
            return "节奏布鲁斯";
        }
        if (containsAny(text, "rap", "hiphop", "hip-hop", "嘻哈")) {
            return "嘻哈说唱";
        }
        if (containsAny(text, "rock")) {
            return "摇滚";
        }

        if (containsAny(text, "随机风格", "随机来点风格")) {
            int randomIndex = (int) (Math.random() * styles.size());
            return styles.get(randomIndex);
        }

        for (String style : styles) {
            if (text.contains(style.toLowerCase(Locale.ROOT))) {
                return style;
            }
        }

        return "";
    }

    private List<String> loadKnownStyleNames() {
        try {
            List<Style> styles = styleMapper.selectList(new QueryWrapper<Style>().select("id", "name"));
            if (styles != null) {
                List<String> names = styles.stream()
                        .map(Style::getName)
                        .filter(item -> item != null && !item.trim().isEmpty())
                        .map(String::trim)
                        .distinct()
                        .toList();
                if (!names.isEmpty()) {
                    return names;
                }
            }
        } catch (Exception ignored) {
        }
        return Arrays.asList("摇滚", "流行", "民谣", "电子", "古风", "治愈", "说唱", "轻音乐", "欧美", "日系", "爵士");
    }

    private boolean matchesSingerStrict(String artistName, String singerConstraint) {
        if (isBlank(artistName) || isBlank(singerConstraint)) {
            return false;
        }
        Set<String> aliasSet = resolveSingerAliases(singerConstraint).stream()
                .map(item -> item.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());

        String artist = artistName.toLowerCase(Locale.ROOT).trim();
        if (aliasSet.contains(artist)) {
            return true;
        }

        // Handle titles like "Coldplay band" / "周杰伦（Jay）" by normalizing separators.
        String normalizedArtist = artist
                .replaceAll("[（）()\\[\\]{}]", " ")
                .replaceAll("(?i)(feat\\.?|ft\\.?|with|x)", " ")
                .replaceAll("[,&/、|;；]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (aliasSet.contains(normalizedArtist)) {
            return true;
        }

        // 处理多歌手分隔场景，目标歌手必须是独立片段
        String[] tokens = normalizedArtist.split("\\s+");
        for (String token : tokens) {
            String normalized = token.trim();
            if (!normalized.isEmpty() && aliasSet.contains(normalized)) {
                return true;
            }
        }

        // CJK aliases usually do not rely on spaces, allow containment as final strict fallback.
        for (String alias : aliasSet) {
            if (alias.matches(".*[\\u4e00-\\u9fa5].*") && alias.length() >= 2 && normalizedArtist.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExplicitSingerSearch(String input) {
        if (isBlank(input)) {
            return false;
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        return text.contains("歌手")
                || text.matches(".*的\\s*(歌|歌曲|音乐).*")
                || text.matches("^(播放|点播|来一首|我要听|帮我放)\\s*.+\\s*的\\s*.+");
    }

    private List<SongVO> filterSongsBySingerStrict(List<SongVO> candidates, String singerConstraint) {
        if (candidates == null || candidates.isEmpty() || isBlank(singerConstraint)) {
            return List.of();
        }
        Set<Long> singerArtistIds = resolveSingerArtistIds(singerConstraint);
        List<SongVO> idMatched = singerArtistIds.isEmpty()
                ? List.of()
                : candidates.stream()
                .filter(song -> song != null && song.getArtistId() != null && singerArtistIds.contains(song.getArtistId()))
                .toList();
        if (!idMatched.isEmpty()) {
            return idMatched;
        }
        // Fallback keeps current strict text behavior when id mapping is unavailable.
        return candidates.stream()
                .filter(song -> song != null && matchesSingerStrict(song.getArtistName(), singerConstraint))
                .toList();
    }

    private List<SongVO> searchSongsBySingerStrict(String singerConstraint, int limit) {
        if (isBlank(singerConstraint) || limit <= 0) {
            return List.of();
        }

        int queryLimit = Math.max(limit, 20);
        Set<Long> artistIds = resolveSingerArtistIds(singerConstraint);
        Map<String, SongVO> merged = new LinkedHashMap<>();

        if (!artistIds.isEmpty()) {
            List<SongVO> byArtistIds = songMapper.searchSongsByArtistIds(new ArrayList<>(artistIds), queryLimit);
            if (byArtistIds != null) {
                for (SongVO song : byArtistIds) {
                    if (song == null) {
                        continue;
                    }
                    String key = song.getSongId() == null
                            ? (safe(song.getSongName()) + "|" + safe(song.getArtistName())).toLowerCase(Locale.ROOT)
                            : "id:" + song.getSongId();
                    merged.putIfAbsent(key, song);
                }
            }
        }

        // Name-based fallback still remains for alias mismatch scenarios.
        if (merged.isEmpty()) {
            List<String> singerQueries = new ArrayList<>(resolveSingerAliases(singerConstraint));
            for (String query : singerQueries) {
                if (isBlank(query)) {
                    continue;
                }
                List<SongVO> rows = songMapper.searchSongsByKeyword(query, queryLimit);
                if (rows == null) {
                    continue;
                }
                for (SongVO row : rows) {
                    if (row == null) {
                        continue;
                    }
                    String key = row.getSongId() == null
                            ? (safe(row.getSongName()) + "|" + safe(row.getArtistName())).toLowerCase(Locale.ROOT)
                            : "id:" + row.getSongId();
                    merged.putIfAbsent(key, row);
                }
            }
        }

        return filterSongsBySingerStrict(new ArrayList<>(merged.values()), singerConstraint).stream()
                .limit(limit)
                .toList();
    }

    private Set<Long> resolveSingerArtistIds(String singerConstraint) {
        Set<Long> artistIds = new LinkedHashSet<>();
        if (isBlank(singerConstraint)) {
            return artistIds;
        }
        try {
            artistIds.addAll(artistAliasResolver.resolveArtistIds(singerConstraint));
            List<String> aliases = new ArrayList<>(resolveSingerAliases(singerConstraint));
            if (aliases.isEmpty()) {
                return artistIds;
            }
            List<Long> rows = artistMapper.findArtistIdsByNames(aliases);
            if (rows != null) {
                rows.stream().filter(item -> item != null && item > 0).forEach(artistIds::add);
            }

            // Fuzzy fallback for alias variants not exactly stored in tb_artist.name.
            for (String alias : aliases) {
                if (isBlank(alias)) {
                    continue;
                }
                List<Long> fuzzyRows = artistMapper.findArtistIdsByKeyword(alias.trim(), 6);
                if (fuzzyRows != null) {
                    fuzzyRows.stream().filter(item -> item != null && item > 0).forEach(artistIds::add);
                }
            }
        } catch (Exception ignored) {
        }
        return artistIds;
    }

    private List<String> resolveArtistNameCandidates(String keyword, int limit) {
        if (isBlank(keyword) || limit <= 0) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>(resolveSingerAliases(keyword));
        try {
            QueryWrapper<Artist> wrapper = new QueryWrapper<Artist>()
                    .select("id", "name")
                    .like("name", keyword)
                    .last("LIMIT " + Math.min(limit, 10));
            List<Artist> artists = artistMapper.selectList(wrapper);
            if (artists != null) {
                artists.stream()
                        .map(Artist::getArtistName)
                        .filter(item -> item != null && !item.trim().isEmpty())
                        .forEach(names::add);
            }
        } catch (Exception ignored) {
        }
        return names.stream().limit(limit).toList();
    }

    private Set<String> resolveSingerAliases(String singerConstraint) {
        Set<String> aliases = new LinkedHashSet<>();
        if (isBlank(singerConstraint)) {
            return aliases;
        }

        String value = singerConstraint.trim();
        aliases.add(value);
        aliases.add(value.replaceAll("[·•・]", "").trim());
        aliases.add(value.replaceAll("\\s+", " ").trim());

        // 从本地歌手库动态扩展别名，提升中英文/大小写/写法差异命中率。
        try {
            List<Artist> exactArtists = artistMapper.selectList(new QueryWrapper<Artist>()
                    .select("id", "name")
                    .eq("name", value)
                    .last("LIMIT 8"));
            if (exactArtists != null) {
                exactArtists.stream()
                        .map(Artist::getArtistName)
                        .filter(item -> item != null && !item.trim().isEmpty())
                        .map(String::trim)
                        .forEach(aliases::add);
            }

            List<Artist> fuzzyArtists = artistMapper.selectList(new QueryWrapper<Artist>()
                    .select("id", "name")
                    .like("name", value)
                    .last("LIMIT 12"));
            if (fuzzyArtists != null) {
                fuzzyArtists.stream()
                        .map(Artist::getArtistName)
                        .filter(item -> item != null && !item.trim().isEmpty())
                        .map(String::trim)
                        .forEach(aliases::add);
            }
        } catch (Exception ignored) {
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if ("coldplay".equals(lower) || "酷玩".equals(value) || "酷玩乐队".equals(value)) {
            aliases.add("Coldplay");
            aliases.add("酷玩");
            aliases.add("酷玩乐队");
        }

        return aliases;
    }

    private String matchKnownStyle(String rawStyle, List<String> styles) {
        if (isBlank(rawStyle) || styles == null || styles.isEmpty()) {
            return "";
        }
        String keyword = rawStyle.trim().toLowerCase(Locale.ROOT);
        for (String style : styles) {
            if (isBlank(style)) {
                continue;
            }
            String styleName = style.trim().toLowerCase(Locale.ROOT);
            if (styleName.equals(keyword) || styleName.contains(keyword) || keyword.contains(styleName)) {
                return style.trim();
            }
        }
        return "";
    }

    private String extractArtistConstraint(String input) {
        if (isBlank(input)) {
            return "";
        }

        String text = input.trim().replaceAll("[，,。！？!?]", " ").replaceAll("\\s+", " ").trim();
        Matcher playlistMatcher = Pattern.compile("(?:推荐|生成|制作|做个|做一个|给我|帮我|来个|来一份|来一张)?\\s*(?:一个|一份|一张|一套)?\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,30})\\s*的?\\s*歌单", Pattern.CASE_INSENSITIVE).matcher(text);
        if (playlistMatcher.find()) {
            String candidate = sanitizeSingerKeyword(playlistMatcher.group(1));
            if (isGenericPlaylistWord(candidate) || shouldTreatAsStyleCandidate(candidate, text)) {
                return "";
            }
            return candidate;
        }

        Matcher singerPlaylistMatcher = Pattern.compile("歌手\\s*([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,30})\\s*的?\\s*歌单", Pattern.CASE_INSENSITIVE).matcher(text);
        if (singerPlaylistMatcher.find()) {
            String candidate = sanitizeSingerKeyword(singerPlaylistMatcher.group(1));
            if (isGenericPlaylistWord(candidate) || shouldTreatAsStyleCandidate(candidate, text)) {
                return "";
            }
            return candidate;
        }

        if (text.contains("只要")) {
            String candidate = text.substring(text.indexOf("只要") + 2)
                    .replaceAll("的?歌单.*$", "")
                    .replaceAll("的?歌.*$", "")
                    .trim();
            candidate = sanitizeSingerKeyword(candidate);
            if (candidate.length() >= 2 && !isGenericPlaylistWord(candidate) && !shouldTreatAsStyleCandidate(candidate, text)) {
                return candidate;
            }
        }

        if (text.contains("歌手")) {
            String candidate = text.substring(text.indexOf("歌手") + 2)
                    .replaceAll("的?歌单.*$", "")
                    .replaceAll("的?歌.*$", "")
                    .trim();
            candidate = sanitizeSingerKeyword(candidate);
            if (candidate.length() >= 2 && !isGenericPlaylistWord(candidate) && !shouldTreatAsStyleCandidate(candidate, text)) {
                return candidate;
            }
        }

        Matcher fallback = Pattern.compile("^([\\u4e00-\\u9fa5A-Za-z0-9·\\-\\s]{2,30})\\s*的?\\s*歌单$", Pattern.CASE_INSENSITIVE).matcher(text);
        if (fallback.find()) {
            String candidate = sanitizeSingerKeyword(fallback.group(1));
            if (candidate.length() >= 2 && !isGenericPlaylistWord(candidate) && !shouldTreatAsStyleCandidate(candidate, text)) {
                return candidate;
            }
        }

        return "";
    }

    private boolean shouldTreatAsStyleCandidate(String candidate, String fullText) {
        if (isBlank(candidate)) {
            return false;
        }
        String normalized = candidate.trim();
        if (normalized.contains("风格") || normalized.toLowerCase(Locale.ROOT).contains("style")) {
            return true;
        }
        if (!isBlank(resolveStyleConstraint(normalized))) {
            return true;
        }
        return !isBlank(resolveStyleConstraint(fullText));
    }

    private String sanitizeSingerKeyword(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        return raw.trim()
                .replaceAll("(?i)^(有没有|有吗|还有吗|是否有|能不能|可不可以|可以吗)\\s*", "")
                .replaceAll("(?i)^(推荐|生成|制作|做个|做一个|给我|帮我|来点|来个|来一份|来一张)", "")
                .replaceAll("(?i)^(一个|一份|一张|一套)", "")
                .replaceAll("(?i)(歌单|playlist|歌曲|音乐)$", "")
                .replaceAll("的$", "")
                .replaceAll("^[\\s:：-]+|[\\s:：-]+$", "")
                .trim();
    }

    private boolean isGenericPlaylistWord(String candidate) {
        if (isBlank(candidate)) {
            return true;
        }
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("推荐")
                || normalized.equals("歌单")
                || normalized.equals("生成")
                || normalized.equals("制作")
                || normalized.equals("来个")
                || normalized.equals("做个")
                || normalized.equals("一个")
                || normalized.equals("一些")
                || normalized.equals("一份")
                || normalized.equals("一张")
                || normalized.equals("某歌手")
                || normalized.equals("某位歌手")
                || normalized.equals("一个歌手")
                || normalized.equals("音乐")
                || normalized.equals("歌曲");
    }

    private List<AgentChatResponseVO.AgentSongCardVO> organizePlaylistSeeds(List<AgentChatRequestDTO.PlaylistSeedDTO> seeds, int limit) {
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }

        return seeds.stream()
                .sorted((a, b) -> {
                    String aStyle = a.getStyle() == null ? "" : a.getStyle();
                    String bStyle = b.getStyle() == null ? "" : b.getStyle();
                    int styleCompare = aStyle.compareToIgnoreCase(bStyle);
                    if (styleCompare != 0) {
                        return styleCompare;
                    }
                    String aArtist = a.getArtistName() == null ? "" : a.getArtistName();
                    String bArtist = b.getArtistName() == null ? "" : b.getArtistName();
                    return aArtist.compareToIgnoreCase(bArtist);
                })
                .limit(limit)
                .map(item -> AgentChatResponseVO.AgentSongCardVO.builder()
                        .songId(item.getSongId())
                        .songName(item.getSongName())
                        .artistName(item.getArtistName())
                        .album("")
                        .coverUrl("")
                        .audioUrl("")
                        .source("library")
                        .reason("按风格与歌手分组后的优先项")
                        .build())
                .toList();
    }

    private TextPair buildAgentNarration(String input,
                                         String intent,
                                         String toolSummary,
                                         AgentChatRequestDTO.NowPlayingDTO nowPlaying,
                                         String ragPromptContext) {
        ChatRequestDTO requestDTO = new ChatRequestDTO();
        String nowPlayingText = "";
        if (nowPlaying != null && !isBlank(nowPlaying.getTitle())) {
            nowPlayingText = "\\n当前播放: " + nowPlaying.getTitle() + " - " + nowPlaying.getArtist();
        }
        String ragText = isBlank(ragPromptContext) ? "" : "\\n" + ragPromptContext;

        requestDTO.setMessage("你是音乐助手Agent。输出简洁、有行动建议。\\n"
                + "intent=" + intent + "\\n"
                + "用户输入=" + input + "\\n"
                + "工具摘要=" + toolSummary
                + nowPlayingText
                + ragText
                + "\\n请返回JSON: {\"chinese\":\"...\",\"japanese\":\"...\"}，中文不超过120字，日文用于语音朗读。"
        );

        String raw = deepSeekService.chat(requestDTO);
        if (!isBlank(raw)) {
            try {
                Map<String, String> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
                String chinese = parsed.getOrDefault("chinese", raw);
                String japanese = parsed.getOrDefault("japanese", "");
                return new TextPair(chinese, japanese);
            } catch (Exception ignored) {
                return new TextPair(raw, "");
            }
        }

        return new TextPair("我已完成处理，你可以继续告诉我下一步。", "");
    }

    private List<AgentChatResponseVO.AgentSongCardVO> mergeSongs(List<AgentChatResponseVO.AgentSongCardVO> local,
                                                                 List<AgentChatResponseVO.AgentSongCardVO> web,
                                                                 int limit) {
        Set<String> dedupe = new LinkedHashSet<>();
        List<AgentChatResponseVO.AgentSongCardVO> merged = new ArrayList<>();
        for (AgentChatResponseVO.AgentSongCardVO item : local) {
            String key = buildSongKey(item);
            if (dedupe.add(key)) {
                merged.add(item);
            }
        }
        for (AgentChatResponseVO.AgentSongCardVO item : web) {
            if (merged.size() >= limit) {
                break;
            }
            String key = buildSongKey(item);
            if (dedupe.add(key)) {
                merged.add(item);
            }
        }
        return merged.stream().limit(limit).toList();
    }

    private List<AgentChatResponseVO.AgentSongCardVO> toSongCards(List<SongVO> songs,
                                                                   String source,
                                                                   String reason,
                                                                   int limit) {
        if (songs == null) {
            return List.of();
        }
        return songs.stream().limit(limit).map(song -> AgentChatResponseVO.AgentSongCardVO.builder()
                        .songId(song.getSongId())
                        .songName(song.getSongName())
                        .artistName(song.getArtistName())
                        .album(song.getAlbum())
                        .coverUrl(song.getCoverUrl())
                        .audioUrl(song.getAudioUrl())
                        .source(source)
                        .reason(reason)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AgentChatResponseVO.AgentPlaylistCardVO> toPlaylistCards(List<PlaylistVO> playlists,
                                                                           String source,
                                                                           String reason,
                                                                           int limit) {
        if (playlists == null) {
            return List.of();
        }
        return playlists.stream().limit(limit).map(item -> AgentChatResponseVO.AgentPlaylistCardVO.builder()
                        .playlistId(item.getPlaylistId())
                        .title(item.getTitle())
                        .coverUrl(item.getCoverUrl())
                        .source(source)
                        .reason(reason)
                        .songCount(null)
                        .tracks(List.of())
                        .build())
                .toList();
    }

    private Long resolveUserId() {
        try {
            Map<String, Object> map = ThreadLocalUtil.get();
            if (map == null) {
                return null;
            }
            Object value = map.get(JwtClaimsConstant.USER_ID);
            if (value == null) {
                return null;
            }
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveUserIdFromRequest(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || token.isEmpty()) {
                return null;
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Object userId = claims.get(JwtClaimsConstant.USER_ID);
            return userId == null ? null : Long.parseLong(userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String buildUniquePlaylistTitle(String baseTitle) {
        String candidate = baseTitle;
        int suffix = 2;
        while (playlistMapper.selectCount(new QueryWrapper<Playlist>().eq("title", candidate)) > 0) {
            candidate = baseTitle + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private void autoCollectPlaylist(Long userId, Long playlistId) {
        QueryWrapper<UserFavorite> queryWrapper = new QueryWrapper<UserFavorite>()
                .eq("user_id", userId)
                .eq("type", 1)
                .eq("playlist_id", playlistId);
        if (userFavoriteMapper.selectCount(queryWrapper) > 0) {
            return;
        }

        UserFavorite userFavorite = new UserFavorite();
        userFavorite.setUserId(userId)
                .setType(1)
                .setPlaylistId(playlistId)
                .setCreateTime(LocalDateTime.now());
        userFavoriteMapper.insert(userFavorite);
    }

    private List<AgentChatResponseVO.AgentSongCardVO> getWebSearchCache(String cacheKey) {
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue == null || cacheValue.isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(cacheValue, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void setWebSearchCache(String cacheKey, List<AgentChatResponseVO.AgentSongCardVO> cards) {
        try {
            String cacheValue = objectMapper.writeValueAsString(cards);
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, searchCacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private boolean isCircuitBreakerOpen(String keyword) {
        String openUntilValue = stringRedisTemplate.opsForValue().get(AGENT_WEB_SEARCH_OPEN_PREFIX + keyword);
        if (openUntilValue == null) {
            return false;
        }
        try {
            long openUntil = Long.parseLong(openUntilValue);
            if (System.currentTimeMillis() <= openUntil) {
                return true;
            }
            stringRedisTemplate.delete(AGENT_WEB_SEARCH_OPEN_PREFIX + keyword);
            return false;
        } catch (Exception e) {
            stringRedisTemplate.delete(AGENT_WEB_SEARCH_OPEN_PREFIX + keyword);
            return false;
        }
    }

    private void triggerCircuitBreaker(String keyword) {
        String failKey = AGENT_WEB_SEARCH_FAIL_PREFIX + keyword;
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        stringRedisTemplate.expire(failKey, Math.max(circuitBreakerOpenSeconds, 60), TimeUnit.SECONDS);
        if (failCount != null && failCount >= circuitBreakerFailThreshold) {
            long openUntil = System.currentTimeMillis() + (long) circuitBreakerOpenSeconds * 1000;
            stringRedisTemplate.opsForValue().set(AGENT_WEB_SEARCH_OPEN_PREFIX + keyword, String.valueOf(openUntil), circuitBreakerOpenSeconds, TimeUnit.SECONDS);
        }
    }

    private void resetCircuitBreaker(String keyword) {
        stringRedisTemplate.delete(AGENT_WEB_SEARCH_FAIL_PREFIX + keyword);
        stringRedisTemplate.delete(AGENT_WEB_SEARCH_OPEN_PREFIX + keyword);
    }

    private AgentChatResponseVO.ToolTraceVO toolOk(String tool, String summary) {
        return AgentChatResponseVO.ToolTraceVO.builder().tool(tool).status("ok").summary(summary).build();
    }

    private AgentChatResponseVO.ToolTraceVO toolFail(String tool, String summary) {
        return AgentChatResponseVO.ToolTraceVO.builder().tool(tool).status("fail").summary(summary).build();
    }

    private String buildSongKey(AgentChatResponseVO.AgentSongCardVO item) {
        return ((item.getSongName() == null ? "" : item.getSongName()) + "|" + (item.getArtistName() == null ? "" : item.getArtistName()))
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String shorten(String text) {
        final int max = 18;
        if (text == null) {
            return "新的心情";
        }
        String value = text.trim();
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }

    private boolean containsAny(String input, String... patterns) {
        if (input == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (input.contains(pattern.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private boolean isLikelyDirectMusicSearch(String input) {
        if (isBlank(input)) {
            return false;
        }
        String raw = input.trim();
        if (raw.length() < 2 || raw.length() > 36) {
            return false;
        }
        String text = raw.toLowerCase(Locale.ROOT);
        if (containsAny(text, "为什么", "怎么", "如何", "可以", "能不能", "介绍", "是什么", "是谁", "谢谢", "你好", "多少", "几", "哪里", "何时", "多大")) {
            return false;
        }
        if (containsAny(text, "暂停", "下一首", "上一首", "推荐", "生成歌单", "整理歌单", "解析", "分析")) {
            return false;
        }
        if (raw.matches(".*[。！？?!].*")) {
            return false;
        }
        String keyword = buildSearchKeyword(raw);
        return !isBlank(keyword) && keyword.length() >= 2;
    }

    private boolean isLikelyKnowledgeQuestion(String input) {
        if (isBlank(input)) {
            return false;
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (containsAny(text,
                "为什么", "怎么", "如何", "是什么", "是谁", "多少", "几", "哪里", "哪个", "哪国", "哪年", "多大", "天气", "时间", "日期", "数学", "历史", "地理")) {
            return !containsAny(text, "歌", "歌曲", "歌手", "播放", "歌单", "专辑", "歌词", "曲库");
        }
        return false;
    }

    private boolean shouldUseRagForInput(String input, Intent intent) {
        if (intent == Intent.PLAYER_CONTROL) {
            return false;
        }
        if (intent != Intent.CHAT) {
            return true;
        }
        return isMusicDomainQuery(input);
    }

    private boolean isMusicDomainQuery(String input) {
        if (isBlank(input)) {
            return false;
        }
        String text = input.toLowerCase(Locale.ROOT);
        if (containsAny(text,
                "歌", "歌曲", "歌手", "专辑", "歌词", "曲库", "播放", "点播", "歌单", "风格", "收藏",
                "music", "song", "artist", "album", "playlist", "lyric", "play")) {
            return true;
        }
        // Only treat plain text as music query when it can map to local song/artist keywords.
        if (isLikelyDirectMusicSearch(input)) {
            String keyword = buildSearchKeyword(input);
            boolean hasSongHit = !toSongCards(songMapper.searchSongsByKeyword(keyword, 1), "local", "", 1).isEmpty();
            boolean hasArtistHit = !resolveArtistNameCandidates(keyword, 1).isEmpty();
            return hasSongHit || hasArtistHit;
        }
        return false;
    }

    private boolean isPotentialSingerKeyword(String keyword) {
        if (isBlank(keyword)) {
            return false;
        }
        String text = keyword.trim();
        if (text.length() < 2 || text.length() > 32) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "播放", "推荐", "歌单", "歌词", "专辑", "解析", "介绍")) {
            return false;
        }
        return !text.matches(".*[。！？?!].*");
    }

    private boolean isPlaylistSearchIntent(String input) {
        if (isBlank(input)) {
            return false;
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        return text.contains("歌单") && containsAny(text,
                "搜索歌单", "查歌单", "找歌单", "检索歌单", "歌单搜索", "歌单检索");
    }

    private String buildPlaylistSearchKeyword(String input) {
        if (isBlank(input)) {
            return "";
        }
        String keyword = input.trim()
                .replaceAll("[，,。！？!?]", " ")
                .replaceAll("(?i)(帮我|给我|请|麻烦|搜索|检索|查|找|看看|有吗|有哪些)", " ")
                .replaceAll("(?i)(歌单|playlist)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return keyword.isEmpty() ? input.trim() : keyword;
    }

    private String extractPlayTargetKeyword(String input) {
        if (isBlank(input)) {
            return "";
        }
        return input.trim()
                .replaceAll("(?i)^(播放|点播|来一首|我要听|帮我放)\\s*", "")
                .replaceAll("(?i)(这首歌|这歌|歌曲)$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<AgentChatResponseVO.AgentSongCardVO> rankSongsForPlayTarget(List<AgentChatResponseVO.AgentSongCardVO> songs,
                                                                              String keyword) {
        if (songs == null || songs.isEmpty()) {
            return List.of();
        }
        if (isBlank(keyword)) {
            return songs;
        }
        return songs.stream()
                .sorted((a, b) -> Double.compare(rankScore(b, keyword), rankScore(a, keyword)))
                .toList();
    }

    private double rankScore(AgentChatResponseVO.AgentSongCardVO song, String keyword) {
        if (song == null || isBlank(keyword)) {
            return 0D;
        }
        String k = keyword.toLowerCase(Locale.ROOT);
        String name = safe(song.getSongName()).toLowerCase(Locale.ROOT);
        String artist = safe(song.getArtistName()).toLowerCase(Locale.ROOT);
        String album = safe(song.getAlbum()).toLowerCase(Locale.ROOT);

        double score = 0D;
        if (name.equals(k)) {
            score += 1D;
        } else if (name.startsWith(k)) {
            score += 0.82D;
        } else if (name.contains(k)) {
            score += 0.68D;
        }

        if (artist.equals(k)) {
            score += 0.45D;
        } else if (artist.contains(k)) {
            score += 0.28D;
        }

        if (album.contains(k)) {
            score += 0.12D;
        }
        return score;
    }

    private enum Intent {
        SEARCH_MUSIC,
        SEARCH_PLAYLIST,
        RECOMMEND,
        ANALYZE_NOW_PLAYING,
        CREATE_PLAYLIST,
        ORGANIZE_PLAYLIST,
        PLAYER_CONTROL,
        CHAT
    }

    private record TextPair(String chinese, String japanese) {}

    private record WebSearchResult(List<AgentChatResponseVO.AgentSongCardVO> songs,
                                   boolean degraded,
                                   boolean fromCache,
                                   String summary) {}
}




