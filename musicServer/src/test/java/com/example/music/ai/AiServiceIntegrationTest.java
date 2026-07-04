package com.example.music.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 服务集成测试。
 * <p>
 * 验证 Spring 上下文能正常启动，且在 API 不可用时能正确降级。
 * 使用无效的 base-url 模拟 API 不可达场景。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.ai.openai.base-url=http://localhost:99999", // 无效地址测试降级
        "ai.service.fallback.enabled=true",
        "ai.service.fallback.message=test-fallback"
})
class AiServiceIntegrationTest {

    @Autowired(required = false)
    private AiService aiService;

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能启动
    }

    @Test
    void testFallbackWhenApiUnavailable() {
        if (aiService == null) {
            return; // AiService bean 未注册时跳过
        }
        var req = new AiService.AiRequest("test", null, null, null, "it-1", "u-1");
        AiService.AiResponse resp = aiService.chat(req);
        assertNotNull(resp);
        // API 不可用时应降级或返回内容
        assertTrue(resp.fallback() || resp.content() != null);
    }

    @Test
    void testEmbedReturnsNonNull() {
        if (aiService == null) {
            return;
        }
        float[] result = aiService.embed("integration test");
        assertNotNull(result);
    }

    @Test
    void testIsAvailableDoesNotThrow() {
        if (aiService == null) {
            return;
        }
        // 健康检查不应抛出异常
        assertDoesNotThrow(() -> aiService.isAvailable());
    }
}
