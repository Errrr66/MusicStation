package com.example.music.ai;

import java.util.List;
import java.util.Map;

/**
 * 统一 AI 服务接口
 * <p>
 * 阶段 2-5 的核心抽象：同步/流式对话、工具调用、健康检查、Embedding。
 * 现有 DeepSeekService 保持不变，后续可逐步迁移到本接口。
 */
public interface AiService {

    /** 同步对话 */
    AiResponse chat(AiRequest request);

    /** 流式对话（返回逐 token 的迭代） */
    java.util.stream.Stream<String> chatStream(AiRequest request);

    /** 带工具调用的对话 */
    AiResponse chatWithTools(AiRequest request, List<ToolDefinition> tools);

    /** 健康检查 */
    boolean isAvailable();

    /** 生成 embedding */
    float[] embed(String text);

    record AiRequest(
            String message,
            List<ChatMessage> history,
            String systemPrompt,
            Map<String, Object> options,
            String requestId,
            String userId
    ) {
    }

    record ChatMessage(String role, String content) {
    }

    record AiResponse(
            String content,
            String model,
            long latencyMs,
            int promptTokens,
            int completionTokens,
            String requestId,
            boolean fallback
    ) {
    }

    record ToolDefinition(String name, String description, Map<String, Object> parameters) {
    }
}
