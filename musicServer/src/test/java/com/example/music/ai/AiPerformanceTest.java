package com.example.music.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 服务性能测试。
 * <p>
 * 仅在设置环境变量 {@code PERFORM_PERF_TEST=true} 时运行，
 * 避免在常规 CI 流程中触发大量外部调用。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=${DEEPSEEK_API_KEY:test-key}",
        "ai.service.fallback.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "PERFORM_PERF_TEST", matches = "true")
class AiPerformanceTest {

    @Autowired(required = false)
    private AiService aiService;

    @Test
    void testConcurrentRequests() throws Exception {
        if (aiService == null) {
            return;
        }
        int threads = 10;
        int requestsPerThread = 5;
        int totalRequests = threads * requestsPerThread;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fallback = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    var req = new AiService.AiRequest(
                            "test " + idx, null, null, null, "perf-" + idx, "u-1");
                    AiService.AiResponse resp = aiService.chat(req);
                    if (resp.fallback()) {
                        fallback.incrementAndGet();
                    } else {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    fallback.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(120, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        executor.shutdown();

        System.out.println("=== 性能测试结果 ===");
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功: " + success.get());
        System.out.println("降级/失败: " + fallback.get());
        System.out.println("总耗时: " + elapsed + "ms");
        if (elapsed > 0) {
            System.out.println("平均响应时间: " + (elapsed / totalRequests) + "ms");
            System.out.printf("QPS: %.2f%n", totalRequests * 1000.0 / elapsed);
        }
        System.out.println("全部完成: " + completed);

        assertTrue(completed, "所有请求应在超时时间内完成");
        assertEquals(totalRequests, success.get() + fallback.get());
    }

    @Test
    void testSingleRequestLatency() {
        if (aiService == null) {
            return;
        }
        int iterations = 5;
        long totalLatency = 0;
        int successCount = 0;

        for (int i = 0; i < iterations; i++) {
            var req = new AiService.AiRequest(
                    "latency test " + i, null, null, null, "lat-" + i, "u-1");
            long start = System.currentTimeMillis();
            try {
                AiService.AiResponse resp = aiService.chat(req);
                long latency = System.currentTimeMillis() - start;
                if (!resp.fallback()) {
                    totalLatency += latency;
                    successCount++;
                }
                System.out.printf("请求 #%d 延迟: %dms (fallback=%b)%n",
                        i, latency, resp.fallback());
            } catch (Exception e) {
                System.out.printf("请求 #%d 异常: %s%n", i, e.getMessage());
            }
        }

        if (successCount > 0) {
            System.out.printf("平均延迟: %dms (成功 %d/%d)%n",
                    totalLatency / successCount, successCount, iterations);
        }
    }

    @Test
    void testStreamPerformance() {
        if (aiService == null) {
            return;
        }
        var req = new AiService.AiRequest(
                "stream perf test", null, null, null, "stream-perf", "u-1");
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        AtomicInteger chunkCount = new AtomicInteger(0);

        try {
            aiService.chatStream(req).forEach(chunk -> {
                chunkCount.incrementAndGet();
                sb.append(chunk);
            });
        } catch (Exception e) {
            System.out.println("流式测试异常: " + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("=== 流式性能 ===");
        System.out.println("总耗时: " + elapsed + "ms");
        System.out.println("chunk 数: " + chunkCount.get());
        System.out.println("响应长度: " + sb.length());
    }

    @Test
    void testSustainedLoad() throws Exception {
        if (aiService == null) {
            return;
        }
        int durationSeconds = 10; // 短时长稳定性测试
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch stopLatch = new CountDownLatch(1);
        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fallback = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int threadIdx = t;
            executor.submit(() -> {
                int reqIdx = 0;
                while (stopLatch.getCount() > 0) {
                    try {
                        var req = new AiService.AiRequest(
                                "sustained " + threadIdx + "-" + reqIdx,
                                null, null, null,
                                "sust-" + threadIdx + "-" + reqIdx, "u-1");
                        AiService.AiResponse resp = aiService.chat(req);
                        total.incrementAndGet();
                        if (resp.fallback()) {
                            fallback.incrementAndGet();
                        } else {
                            success.incrementAndGet();
                        }
                    } catch (Exception e) {
                        total.incrementAndGet();
                        fallback.incrementAndGet();
                    }
                    reqIdx++;
                }
            });
        }

        Thread.sleep(durationSeconds * 1000L);
        stopLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("=== 稳定性测试结果 ===");
        System.out.println("持续时间: " + durationSeconds + "s");
        System.out.println("总请求数: " + total.get());
        System.out.println("成功: " + success.get());
        System.out.println("降级/失败: " + fallback.get());
        if (total.get() > 0) {
            System.out.printf("错误率: %.2f%%%n",
                    fallback.get() * 100.0 / total.get());
            System.out.printf("QPS: %.2f%n",
                    total.get() * 1.0 / durationSeconds);
        }
    }
}
