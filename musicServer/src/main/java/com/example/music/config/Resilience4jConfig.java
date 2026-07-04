package com.example.music.config;

import com.example.music.ai.AiServiceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j 配置
 * <p>
 * 从 {@link AiServiceProperties} 读取参数，构建名为 "aiService" 的
 * {@link Retry} 与 {@link CircuitBreaker}，供 SpringAiService 上的
 * {@code @Retry} / {@code @CircuitBreaker} 注解使用。
 */
@Configuration
public class Resilience4jConfig {

    public static final String RETRY_NAME = "aiServiceRetry";
    public static final String CB_NAME = "aiServiceCircuitBreaker";

    @Bean
    public RetryRegistry retryRegistry(AiServiceProperties props) {
        AiServiceProperties.Retry r = props.getRetry();
        long backoff = Math.max(1L, r.getBackoffMillis());
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(Math.max(1, r.getMaxAttempts()))
                .intervalFunction(IntervalFunction.ofExponentialBackoff(backoff, 2.0))
                .retryExceptions(Exception.class)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(AiServiceProperties props) {
        AiServiceProperties.CircuitBreaker cb = props.getCircuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.getFailureRateThreshold())
                .slowCallRateThreshold(cb.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofSeconds(cb.getSlowCallDurationThresholdSeconds()))
                .waitDurationInOpenState(Duration.ofSeconds(cb.getWaitDurationSeconds()))
                .slidingWindowSize(cb.getSlidingWindowSize())
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean(name = RETRY_NAME)
    public Retry aiServiceRetry(RetryRegistry registry) {
        return registry.retry(RETRY_NAME);
    }

    @Bean(name = CB_NAME)
    public CircuitBreaker aiServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CB_NAME);
    }
}
