package com.example.music.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 调用全链路日志
 * 记录请求/响应/异常，支持通过 requestId 追踪
 */
@Slf4j
@Component
public class AiCallLogger {

    public void logRequest(String requestId, String userId, String model,
                           String message, Map<String, Object> options) {
        log.info("[AI-REQ] requestId={}, userId={}, model={}, messageLen={}, options={}",
                requestId, userId, model, message == null ? 0 : message.length(), options);
    }

    public void logResponse(String requestId, String model, long latencyMs,
                            int promptTokens, int completionTokens, boolean fallback) {
        log.info("[AI-RESP] requestId={}, model={}, latencyMs={}, promptTokens={}, completionTokens={}, fallback={}",
                requestId, model, latencyMs, promptTokens, completionTokens, fallback);
    }

    public void logError(String requestId, String phase, Throwable e) {
        log.error("[AI-ERROR] requestId={}, phase={}, error={}", requestId, phase, e.getMessage(), e);
    }

    public void logStream(String requestId, String phase, int chunkIndex, String chunkPreview) {
        log.debug("[AI-STREAM] requestId={}, phase={}, chunk#={}, preview={}",
                requestId, phase, chunkIndex, chunkPreview);
    }
}
