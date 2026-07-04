package com.example.music.ai;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 基于 Spring AI 1.0.0 的统一 AI 服务实现。
 * <p>
 * 容错策略：使用 Resilience4j {@code @Retry} + {@code @CircuitBreaker} 注解，
 * 失败后通过 fallbackMethod 返回降级响应。
 * <p>
 * 注意：不修改现有 DeepSeekService，保持 ChatController 现有 API 兼容，
 * 后续可逐步将调用方迁移到本服务。
 */
@Slf4j
@Service
public class SpringAiService implements AiService {

    private static final String RETRY_NAME = "aiServiceRetry";
    private static final String CB_NAME = "aiServiceCircuitBreaker";

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final AiServiceProperties props;
    private final AiCallLogger callLogger;
    private final ModelConfigManager configManager;

    public SpringAiService(ChatClient chatClient,
                           EmbeddingModel embeddingModel,
                           AiServiceProperties props,
                           AiCallLogger callLogger,
                           ModelConfigManager configManager) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.props = props;
        this.callLogger = callLogger;
        this.configManager = configManager;
    }

    @Override
    @Retry(name = RETRY_NAME, fallbackMethod = "chatFallback")
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "chatFallback")
    public AiResponse chat(AiRequest request) {
        String requestId = resolveRequestId(request);
        String model = resolveModel();
        long start = System.currentTimeMillis();

        callLogger.logRequest(requestId, request.userId(), model, request.message(), request.options());

        try {
            List<Message> messages = buildMessages(request);
            ChatResponse response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            String content = extractContent(response);
            long latency = System.currentTimeMillis() - start;
            int promptTokens = extractTokens(response, true);
            int completionTokens = extractTokens(response, false);

            callLogger.logResponse(requestId, model, latency, promptTokens, completionTokens, false);
            return new AiResponse(content, model, latency, promptTokens, completionTokens, requestId, false);
        } catch (Exception e) {
            callLogger.logError(requestId, "chat", e);
            throw e; // 重新抛出，交由 Retry / CircuitBreaker / fallback 处理
        }
    }

    @Override
    public Stream<String> chatStream(AiRequest request) {
        String requestId = resolveRequestId(request);
        String model = resolveModel();
        callLogger.logRequest(requestId, request.userId(), model, request.message(), request.options());

        try {
            List<Message> messages = buildMessages(request);
            AtomicInteger chunkIndex = new AtomicInteger(0);
            // ChatClient.stream().content() 返回 reactor.core.publisher.Flux<String>，
            // 通过 toStream() 转为阻塞的 java.util.stream.Stream，便于在 MVC 环境下消费。
            return chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .content()
                    .toStream()
                    .peek(chunk -> callLogger.logStream(
                            requestId, "delta", chunkIndex.incrementAndGet(), preview(chunk)));
        } catch (Exception e) {
            callLogger.logError(requestId, "chatStream", e);
            if (props.getFallback().isEnabled()) {
                return Stream.of(props.getFallback().getMessage());
            }
            throw e;
        }
    }

    @Override
    @Retry(name = RETRY_NAME, fallbackMethod = "chatWithToolsFallback")
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "chatWithToolsFallback")
    public AiResponse chatWithTools(AiRequest request, List<ToolDefinition> tools) {
        String requestId = resolveRequestId(request);
        String model = resolveModel();
        long start = System.currentTimeMillis();

        if (tools != null && !tools.isEmpty()) {
            log.info("[AI-TOOLS] requestId={}, tools={}", requestId,
                    tools.stream().map(ToolDefinition::name).toList());
        }

        callLogger.logRequest(requestId, request.userId(), model, request.message(), request.options());

        try {
            // 当前未注册具体的 ToolCallback，先以普通对话返回。
            // 工具回调注册基础设施已就绪，后续可通过 ChatClient.tools(...) 接入。
            List<Message> messages = buildMessages(request);
            ChatResponse response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            String content = extractContent(response);
            long latency = System.currentTimeMillis() - start;
            int promptTokens = extractTokens(response, true);
            int completionTokens = extractTokens(response, false);

            callLogger.logResponse(requestId, model, latency, promptTokens, completionTokens, false);
            return new AiResponse(content, model, latency, promptTokens, completionTokens, requestId, false);
        } catch (Exception e) {
            callLogger.logError(requestId, "chatWithTools", e);
            throw e;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String probe = chatClient.prompt()
                    .user("ping")
                    .call()
                    .content();
            return probe != null;
        } catch (Exception e) {
            log.warn("[AI-HEALTH] provider unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[0];
        }
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            callLogger.logError("embed", "embed", e);
            return new float[0];
        }
    }

    // ====================== fallback 方法 ======================

    @SuppressWarnings("unused")
    public AiResponse chatFallback(AiRequest request, Throwable t) {
        return buildFallback(request, t);
    }

    @SuppressWarnings("unused")
    public AiResponse chatWithToolsFallback(AiRequest request, List<ToolDefinition> tools, Throwable t) {
        return buildFallback(request, t);
    }

    private AiResponse buildFallback(AiRequest request, Throwable t) {
        String requestId = resolveRequestId(request);
        if (!props.getFallback().isEnabled()) {
            if (t instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(t);
        }
        log.warn("[AI-FALLBACK] requestId={}, reason={}", requestId, t.getMessage());
        return new AiResponse(props.getFallback().getMessage(), resolveModel(), 0L, 0, 0, requestId, true);
    }

    // ====================== 辅助方法 ======================

    private List<Message> buildMessages(AiRequest request) {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = request.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (request.history() != null) {
            for (AiService.ChatMessage msg : request.history()) {
                if (msg == null || msg.content() == null) {
                    continue;
                }
                String role = msg.role() == null ? "user" : msg.role().toLowerCase();
                switch (role) {
                    case "system" -> messages.add(new SystemMessage(msg.content()));
                    case "assistant" -> messages.add(new AssistantMessage(msg.content()));
                    default -> messages.add(new UserMessage(msg.content()));
                }
            }
        }
        if (request.message() != null && !request.message().isBlank()) {
            messages.add(new UserMessage(request.message()));
        }
        return messages;
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private int extractTokens(ChatResponse response, boolean prompt) {
        try {
            if (response == null || response.getMetadata() == null) {
                return 0;
            }
            Usage usage = response.getMetadata().getUsage();
            if (usage == null) {
                return 0;
            }
            // Usage 返回 Integer（可能为 null），显式处理避免拆箱 NPE
            Integer tokens = prompt ? usage.getPromptTokens() : usage.getCompletionTokens();
            if (tokens == null || tokens < 0) {
                return 0;
            }
            return tokens;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String resolveModel() {
        return configManager.getConfig("default").model();
    }

    private String resolveRequestId(AiRequest request) {
        if (request == null) {
            return UUID.randomUUID().toString();
        }
        return request.requestId() != null && !request.requestId().isBlank()
                ? request.requestId()
                : UUID.randomUUID().toString();
    }

    private String preview(String chunk) {
        if (chunk == null) {
            return "";
        }
        return chunk.length() <= 40 ? chunk : chunk.substring(0, 40) + "...";
    }
}
