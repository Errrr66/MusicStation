package com.example.music.controller;

import com.example.music.ai.StreamingChatService;
import com.example.music.model.dto.ChatRequestDTO;
import com.example.music.model.dto.AgentChatRequestDTO;
import com.example.music.model.dto.AgentPlaylistSaveDTO;
import com.example.music.model.dto.RagEvalRequestDTO;
import com.example.music.model.vo.AgentChatResponseVO;
import com.example.music.model.vo.ChatHealthVO;
import com.example.music.model.vo.AgentPlaylistSaveVO;
import com.example.music.model.vo.RagEvalVO;
import com.example.music.result.Result;
import com.example.music.service.AgentRagService;
import com.example.music.service.ArtistAliasResolver;
import com.example.music.service.DeepSeekService;
import com.example.music.service.MusicAgentService;
import com.example.music.service.SemanticEmbeddingService;
import com.example.music.service.SpeechService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final int STREAM_CHUNK_SIZE = 12;
    // 复用 ObjectMapper，避免每次请求创建
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ExecutorService STREAM_EXECUTOR = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private int idx = 0;

        @Override
        @SuppressWarnings("NullableProblems")
        public synchronized Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "chat-agent-sse-" + (++idx));
            thread.setDaemon(true);
            return thread;
        }
    });

    @PreDestroy
    public void destroy() {
        STREAM_EXECUTOR.shutdown();
        try {
            if (!STREAM_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                STREAM_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            STREAM_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final DeepSeekService deepSeekService;
    private final SpeechService speechService;
    private final MusicAgentService musicAgentService;
    private final AgentRagService agentRagService;
    private final ArtistAliasResolver artistAliasResolver;
    private final SemanticEmbeddingService semanticEmbeddingService;
    private final StreamingChatService streamingChatService;

    @Value("${agent.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${agent.rag.mode:hybrid}")
    private String ragMode;

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${tts.api-url:}")
    private String ttsApiUrl;

    @Value("${chat.stream.v2.enabled:true}")
    private boolean streamV2Enabled;

    @Autowired
    public ChatController(DeepSeekService deepSeekService,
                          SpeechService speechService,
                          MusicAgentService musicAgentService,
                          AgentRagService agentRagService,
                          ArtistAliasResolver artistAliasResolver,
                          SemanticEmbeddingService semanticEmbeddingService,
                          StreamingChatService streamingChatService) {
        this.deepSeekService = deepSeekService;
        this.speechService = speechService;
        this.musicAgentService = musicAgentService;
        this.agentRagService = agentRagService;
        this.artistAliasResolver = artistAliasResolver;
        this.semanticEmbeddingService = semanticEmbeddingService;
        this.streamingChatService = streamingChatService;
    }

    @GetMapping("/health")
    public Result<ChatHealthVO> health() {
        AgentRagService.RetrievalHealth retrievalHealth = agentRagService.getLastRetrievalHealth();

        ChatHealthVO data = ChatHealthVO.builder()
                .ragEnabled(ragEnabled)
                .ragMode(isBlank(ragMode) ? "hybrid" : ragMode)
                .ragLastRetrieval(ChatHealthVO.RagRetrievalVO.builder()
                        .strategy(retrievalHealth.strategy())
                        .mode(retrievalHealth.mode())
                        .queryCount(retrievalHealth.queryCount())
                        .candidateCount(retrievalHealth.candidateCount())
                        .citationCount(retrievalHealth.citationCount())
                        .enabled(retrievalHealth.enabled())
                        .updatedAtEpochMs(retrievalHealth.updatedAtEpochMs())
                        .build())
                .providers(ChatHealthVO.ProviderStatusVO.builder()
                        .deepseekConfigured(hasValue(deepseekApiKey))
                        .ttsConfigured(hasValue(ttsApiUrl))
                        .semanticConfigured(semanticEmbeddingService.isConfigured())
                        .build())
                .serverTime(java.time.LocalDateTime.now().toString())
                .build();

        return Result.success("Success", data);
    }

    @PostMapping("/ask")
    public Result<Map<String, String>> ask(@RequestBody ChatRequestDTO chatRequestDTO) {
        if ((chatRequestDTO.getMessage() == null || chatRequestDTO.getMessage().isEmpty()) &&
                (chatRequestDTO.getMessages() == null || chatRequestDTO.getMessages().isEmpty())) {
            return Result.error("Message cannot be empty");
        }

        // 调用 AI (Single Request Mode)
        String fullResponse = deepSeekService.chat(chatRequestDTO);
        log.debug("DeepSeek Response: {}", fullResponse);

        String chineseResponse;
        String japaneseResponse;

        // 解析 JSON
        try {
            Map<String, String> jsonMap = MAPPER.readValue(fullResponse, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            chineseResponse = jsonMap.get("chinese");
            japaneseResponse = jsonMap.get("japanese");
        } catch (Exception e) {
            // 解析失败（可能 AI 没有返回 JSON），则直接作为中文文本
            chineseResponse = fullResponse;
            japaneseResponse = "";
            log.warn("JSON parsing failed: {}", e.getMessage(), e);
        }

        // 兜底清洗，确保没有动作描写残留
        if (chineseResponse == null) chineseResponse = "";
        
        // 生成语音
        String audioUrl = "";
        if (japaneseResponse != null && !japaneseResponse.isEmpty()) {
             // 清洗：去掉可能存在的日文括号备注（双保险）
             String cleanJapanese = japaneseResponse.replaceAll("(?s)（.*?）", "").replaceAll("(?s)\\(.*?\\)", "").trim();
             if (!cleanJapanese.isEmpty()) {
                audioUrl = speechService.generateAndUploadSpeech(cleanJapanese, 6);
             }
        }

        Map<String, String> data = new HashMap<>();
        data.put("answer", chineseResponse);
        data.put("audio", audioUrl);

        return Result.success("Success", data);
    }

    @PostMapping("/agent")
    public Result<AgentChatResponseVO> askAgent(@RequestBody AgentChatRequestDTO requestDTO,
                                                HttpServletRequest request) {
        if ((requestDTO.getMessage() == null || requestDTO.getMessage().isEmpty())
                && (requestDTO.getMessages() == null || requestDTO.getMessages().isEmpty())) {
            return Result.error("Message cannot be empty");
        }
        return Result.success("Success", musicAgentService.handle(requestDTO, request));
    }

    @PostMapping("/agent/stream")
    public SseEmitter askAgentStream(@RequestBody AgentChatRequestDTO requestDTO,
                                     HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);

        if ((requestDTO.getMessage() == null || requestDTO.getMessage().isEmpty())
                && (requestDTO.getMessages() == null || requestDTO.getMessages().isEmpty())) {
            try {
                emitter.send(SseEmitter.event().name("error").data("Message cannot be empty"));
            } catch (Exception e) {
                log.warn("SSE 发送空消息错误失败：{}", e.getMessage(), e);
            }
            emitter.complete();
            return emitter;
        }

        emitter.onTimeout(emitter::complete);

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data("ok"));
                AgentChatResponseVO responseVO = musicAgentService.handle(requestDTO, request);
                String answer = responseVO.getAnswer() == null ? "" : responseVO.getAnswer();
                for (String chunk : splitAnswer(answer)) {
                    emitter.send(SseEmitter.event().name("delta").data(chunk));
                }
                String donePayload = MAPPER.writeValueAsString(responseVO);
                emitter.send(SseEmitter.event().name("done").data(donePayload));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("Stream failed: " + e.getMessage()));
                } catch (Exception ex) {
                    log.warn("SSE 发送错误事件失败：{}", ex.getMessage(), ex);
                }
                emitter.completeWithError(e);
            }
        }, STREAM_EXECUTOR);

        return emitter;
    }

    /**
     * 真流式 SSE 端点（阶段6）
     * <p>
     * 与 {@link #askAgentStream}（伪流式：先全量调用再按 12 字符切片）不同，
     * 本端点利用 Spring AI ChatClient 的 stream() 能力逐 token 推送 delta 事件。
     * <p>
     * 简化实现：
     * <ol>
     *   <li>同步执行 Agent 编排（{@code MusicAgentService.handle}）获取 intent/toolTrace/songs/playlists/citations</li>
     *   <li>对 narration 部分使用 {@link StreamingChatService} 真流式生成</li>
     *   <li>逐 token 发送 delta 事件</li>
     *   <li>流结束后发送 done 事件（含完整 VO，answer 为流式累积文本）</li>
     * </ol>
     * SSE 事件名（start/delta/done/error）与旧端点保持兼容。
     */
    @PostMapping("/agent/stream/v2")
    public SseEmitter askAgentStreamV2(@RequestBody AgentChatRequestDTO requestDTO,
                                       HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);

        if ((requestDTO.getMessage() == null || requestDTO.getMessage().isEmpty())
                && (requestDTO.getMessages() == null || requestDTO.getMessages().isEmpty())) {
            try {
                emitter.send(SseEmitter.event().name("error").data("Message cannot be empty"));
            } catch (Exception e) {
                log.warn("SSE v2 发送空消息错误失败：{}", e.getMessage(), e);
            }
            emitter.complete();
            return emitter;
        }

        emitter.onTimeout(emitter::complete);
        emitter.onError(throwable -> log.warn("SSE v2 异常：{}", throwable.getMessage()));

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data("ok"));

                // 1) 同步执行 Agent 编排，获取意图/工具轨迹/歌曲/歌单/引用
                AgentChatResponseVO orchestrationVO = musicAgentService.handle(requestDTO, request);
                String intent = orchestrationVO.getIntent() == null ? "CHAT" : orchestrationVO.getIntent();
                String userInput = resolveUserInput(requestDTO);
                String toolSummary = buildToolSummary(orchestrationVO.getToolTrace());

                // 播放器控制意图：直接发送固定文案，无需流式
                if ("PLAYER_CONTROL".equals(intent)) {
                    String fixedAnswer = orchestrationVO.getAnswer() == null ? "" : orchestrationVO.getAnswer();
                    if (!fixedAnswer.isEmpty()) {
                        emitter.send(SseEmitter.event().name("delta").data(fixedAnswer));
                    }
                    sendDone(emitter, orchestrationVO);
                    emitter.complete();
                    return;
                }

                // 2) 构建流式 narration 提示，使用 Spring AI 真流式生成
                String systemPrompt = "你是音乐助手Agent。输出简洁、有行动建议，使用中文，不超过120字。";
                StringBuilder userPrompt = new StringBuilder();
                userPrompt.append("intent=").append(intent).append("\n")
                        .append("用户输入=").append(userInput).append("\n")
                        .append("工具摘要=").append(toolSummary).append("\n");
                if (requestDTO.getNowPlaying() != null
                        && requestDTO.getNowPlaying().getTitle() != null
                        && !requestDTO.getNowPlaying().getTitle().isEmpty()) {
                    userPrompt.append("当前播放: ")
                            .append(requestDTO.getNowPlaying().getTitle())
                            .append(" - ")
                            .append(requestDTO.getNowPlaying().getArtist())
                            .append("\n");
                }
                if (!orchestrationVO.getSongs().isEmpty()) {
                    userPrompt.append("已召回歌曲数: ").append(orchestrationVO.getSongs().size()).append("\n");
                }
                if (!orchestrationVO.getPlaylists().isEmpty()) {
                    userPrompt.append("已召回歌单数: ").append(orchestrationVO.getPlaylists().size()).append("\n");
                }
                userPrompt.append("请直接给出中文回答（不要返回 JSON，不要包含动作描写）。");

                // 3) 订阅 Flux，逐 token 发送 delta；流式未启用或失败则回退到编排答案
                StringBuilder accumulated = new StringBuilder();
                boolean streamed = false;
                try {
                    if (streamV2Enabled) {
                        java.util.stream.Stream<String> tokenStream = streamingChatService
                                .streamChat(systemPrompt, userPrompt.toString(), null)
                                .toStream();
                        java.util.Iterator<String> iterator = tokenStream.iterator();
                        while (iterator.hasNext()) {
                            String token = iterator.next();
                            if (token == null || token.isEmpty()) {
                                continue;
                            }
                            accumulated.append(token);
                            emitter.send(SseEmitter.event().name("delta").data(token));
                            streamed = true;
                        }
                    }
                } catch (Exception streamEx) {
                    log.warn("SSE v2 流式生成失败，回退到编排答案：{}", streamEx.getMessage());
                }

                // 4) 流式未产出任何 token 时，回退使用编排阶段的 answer
                if (!streamed) {
                    String fallback = orchestrationVO.getAnswer() == null ? "" : orchestrationVO.getAnswer();
                    if (!fallback.isEmpty()) {
                        emitter.send(SseEmitter.event().name("delta").data(fallback));
                    }
                    accumulated.setLength(0);
                    accumulated.append(fallback);
                }

                // 5) 组装最终 VO 并发送 done 事件（answer 用流式累积文本，audio 简化为空）
                AgentChatResponseVO finalVO = AgentChatResponseVO.builder()
                        .answer(accumulated.toString())
                        .audio("")
                        .intent(orchestrationVO.getIntent())
                        .playerCommand(orchestrationVO.getPlayerCommand())
                        .toolTrace(orchestrationVO.getToolTrace())
                        .songs(orchestrationVO.getSongs())
                        .playlists(orchestrationVO.getPlaylists())
                        .citations(orchestrationVO.getCitations())
                        .musicArchive(orchestrationVO.getMusicArchive())
                        .build();
                sendDone(emitter, finalVO);
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("Stream failed: " + e.getMessage()));
                } catch (Exception ex) {
                    log.warn("SSE v2 发送错误事件失败：{}", ex.getMessage(), ex);
                }
                emitter.completeWithError(e);
            }
        }, STREAM_EXECUTOR);

        return emitter;
    }

    private void sendDone(SseEmitter emitter, AgentChatResponseVO vo) {
        try {
            String donePayload = MAPPER.writeValueAsString(vo);
            emitter.send(SseEmitter.event().name("done").data(donePayload));
        } catch (Exception e) {
            log.warn("SSE 发送 done 事件失败：{}", e.getMessage(), e);
        }
    }

    private String resolveUserInput(AgentChatRequestDTO requestDTO) {
        if (requestDTO.getMessage() != null && !requestDTO.getMessage().isEmpty()) {
            return requestDTO.getMessage().trim();
        }
        if (requestDTO.getMessages() != null && !requestDTO.getMessages().isEmpty()) {
            for (int i = requestDTO.getMessages().size() - 1; i >= 0; i--) {
                AgentChatRequestDTO.ChatMessageDTO item = requestDTO.getMessages().get(i);
                if (item != null && "user".equalsIgnoreCase(item.getRole())
                        && item.getContent() != null && !item.getContent().isEmpty()) {
                    return item.getContent().trim();
                }
            }
        }
        return "";
    }

    private String buildToolSummary(List<AgentChatResponseVO.ToolTraceVO> toolTrace) {
        if (toolTrace == null || toolTrace.isEmpty()) {
            return "无工具调用";
        }
        StringBuilder sb = new StringBuilder();
        for (AgentChatResponseVO.ToolTraceVO trace : toolTrace) {
            if (trace == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(trace.getTool() == null ? "unknown" : trace.getTool())
                    .append("(").append(trace.getStatus() == null ? "?" : trace.getStatus()).append(")")
                    .append(": ").append(trace.getSummary() == null ? "" : trace.getSummary());
        }
        return sb.toString();
    }

    @PostMapping("/agent/savePlaylist")
    public Result<AgentPlaylistSaveVO> saveAgentPlaylist(@RequestBody AgentPlaylistSaveDTO saveDTO,
                                                         HttpServletRequest request) {
        return Result.success("Success", musicAgentService.saveAgentPlaylist(saveDTO, request));
    }

    @PostMapping("/agent/artist-alias/refresh")
    public Result<Map<String, Object>> refreshArtistAliasIndex() {
        return Result.success("Success", artistAliasResolver.refreshAliasIndex());
    }

    @PostMapping("/rag/evaluate")
    public Result<RagEvalVO> evaluateRag(@RequestBody RagEvalRequestDTO requestDTO) {
        return Result.success("Success", agentRagService.evaluate(requestDTO));
    }

    private List<String> splitAnswer(String text) {
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        for (int i = 0; i < text.length(); i += STREAM_CHUNK_SIZE) {
            int end = Math.min(i + STREAM_CHUNK_SIZE, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
