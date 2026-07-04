package com.example.music.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SpringAiService 单元测试。
 * <p>
 * 使用 Mockito 模拟 ChatClient / EmbeddingModel，验证：
 * - 正常调用返回响应
 * - 异常降级逻辑（直接测试 chatFallback 方法）
 * - 流式调用返回多个 chunk
 * - embedding 调用
 * <p>
 * 注意：{@code @Retry}/{@code @CircuitBreaker} 注解在纯单元测试中不生效
 * （无 Spring AOP 代理），因此通过直接调用 fallback 方法测试降级逻辑。
 */
class SpringAiServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private ChatClient.StreamResponseSpec streamResponseSpec;
    private EmbeddingModel embeddingModel;
    private AiServiceProperties properties;
    private AiCallLogger logger;
    private ModelConfigManager configManager;
    private SpringAiService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
        embeddingModel = mock(EmbeddingModel.class);
        properties = new AiServiceProperties();
        logger = new AiCallLogger();
        configManager = mock(ModelConfigManager.class);

        ModelConfigManager.ModelConfig modelConfig = new ModelConfigManager.ModelConfig(
                "test-model", 0.7, 2048, 1.0, 1L, System.currentTimeMillis());
        when(configManager.getConfig("default")).thenReturn(modelConfig);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);

        service = new SpringAiService(chatClient, embeddingModel, properties, logger, configManager);
    }

    @Test
    void testChatSuccess() {
        // 使用 RETURNS_DEEP_STUBS 简化 ChatResponse -> Generation -> AssistantMessage 的链式 mock
        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("Hello from AI");
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        var request = new AiService.AiRequest("hi", null, "sys", null, "req-1", "u-1");
        var resp = service.chat(request);

        assertNotNull(resp);
        assertEquals("Hello from AI", resp.content());
        assertEquals("test-model", resp.model());
        assertEquals("req-1", resp.requestId());
        assertFalse(resp.fallback());
    }

    @Test
    void testChatWithHistory() {
        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("OK");
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);

        var history = List.of(
                new AiService.ChatMessage("user", "previous question"),
                new AiService.ChatMessage("assistant", "previous answer")
        );
        var request = new AiService.AiRequest("follow up", history, "sys", null, "req-2", "u-1");
        var resp = service.chat(request);

        assertNotNull(resp);
        assertEquals("OK", resp.content());
    }

    @Test
    void testChatThrowsWhenChatClientFails() {
        // 纯单元测试中无 Spring AOP 代理，@Retry/@CircuitBreaker 不生效。
        // chat() 方法捕获异常后重新抛出，fallback 仅由 Resilience4j 切面在生产环境调用。
        when(requestSpec.call()).thenThrow(new RuntimeException("API down"));

        var request = new AiService.AiRequest("hi", null, "sys", null, "req-1", "u-1");
        assertThrows(RuntimeException.class, () -> service.chat(request));
    }

    @Test
    void testChatFallbackMethod() {
        // 直接测试 fallback 方法（生产环境由 Resilience4j 切面调用）
        var request = new AiService.AiRequest("hi", null, "sys", null, "req-1", "u-1");
        AiService.AiResponse resp = service.chatFallback(request, new RuntimeException("API down"));

        assertNotNull(resp);
        assertTrue(resp.fallback());
        assertEquals(properties.getFallback().getMessage(), resp.content());
        assertEquals("req-1", resp.requestId());
    }

    @Test
    void testChatFallbackDisabledRethrows() {
        properties.getFallback().setEnabled(false);
        var request = new AiService.AiRequest("hi", null, "sys", null, "req-1", "u-1");
        assertThrows(RuntimeException.class, () ->
                service.chatFallback(request, new RuntimeException("API down")));
    }

    @Test
    void testChatWithToolsFallback() {
        var request = new AiService.AiRequest("hi", null, "sys", null, "req-tools", "u-1");
        var tools = List.<AiService.ToolDefinition>of();
        AiService.AiResponse resp = service.chatWithToolsFallback(request, tools, new RuntimeException("down"));

        assertNotNull(resp);
        assertTrue(resp.fallback());
        assertEquals(properties.getFallback().getMessage(), resp.content());
    }

    @Test
    void testChatStream() {
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(
                Flux.fromIterable(List.of("chunk1", "chunk2", "chunk3")));

        var request = new AiService.AiRequest("hi", null, null, null, "stream-1", "u-1");
        List<String> chunks = service.chatStream(request).toList();

        assertEquals(3, chunks.size());
        assertEquals("chunk1", chunks.get(0));
        assertEquals("chunk2", chunks.get(1));
        assertEquals("chunk3", chunks.get(2));
    }

    @Test
    void testChatStreamFallbackOnException() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("API down"));

        var request = new AiService.AiRequest("hi", null, null, null, "stream-2", "u-1");
        List<String> chunks = service.chatStream(request).toList();

        assertEquals(1, chunks.size());
        assertEquals(properties.getFallback().getMessage(), chunks.get(0));
    }

    @Test
    void testChatStreamFallbackDisabledRethrows() {
        properties.getFallback().setEnabled(false);
        when(chatClient.prompt()).thenThrow(new RuntimeException("API down"));

        var request = new AiService.AiRequest("hi", null, null, null, "stream-3", "u-1");
        assertThrows(RuntimeException.class, () -> service.chatStream(request));
    }

    @Test
    void testEmbed() {
        float[] expected = {0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed("hello")).thenReturn(expected);

        float[] result = service.embed("hello");
        assertArrayEquals(expected, result);
    }

    @Test
    void testEmbedEmptyText() {
        float[] result = service.embed("");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testEmbedNullText() {
        float[] result = service.embed(null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testEmbedFallbackOnException() {
        when(embeddingModel.embed("fail")).thenThrow(new RuntimeException("embed error"));
        float[] result = service.embed("fail");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testIsAvailable() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("pong");

        assertTrue(service.isAvailable());
    }

    @Test
    void testIsAvailableReturnsFalseOnException() {
        when(requestSpec.call()).thenThrow(new RuntimeException("API down"));

        assertFalse(service.isAvailable());
    }

    @Test
    void testIsAvailableReturnsFalseOnNullContent() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(null);

        assertFalse(service.isAvailable());
    }
}
