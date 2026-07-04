package com.example.music.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一 AI 服务配置属性
 * 绑定 application.yml 中 ai.service.* 前缀配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    private String defaultProvider = "openai-compatible";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private Fallback fallback = new Fallback();
    private Logging logging = new Logging();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffMillis = 1000;
    }

    @Data
    public static class CircuitBreaker {
        private float failureRateThreshold = 0.5f;
        private float slowCallRateThreshold = 0.5f;
        private int slowCallDurationThresholdSeconds = 30;
        private int waitDurationSeconds = 60;
        private int slidingWindowSize = 10;
    }

    @Data
    public static class Fallback {
        private boolean enabled = true;
        private String message = "AI 服务暂时不可用，请稍后重试";
    }

    @Data
    public static class Logging {
        private boolean enabled = true;
        private boolean logRequestBody = true;
        private boolean logResponseBody = false;
    }
}
