package com.example.music.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelConfigManager 单元测试。
 * 验证默认配置、更新后 version 递增、配置列表。
 * <p>
 * 因字段使用 @Value 注入，通过 ReflectionTestUtils 设置字段后调用 init()。
 */
class ModelConfigManagerTest {

    private ModelConfigManager manager;

    @BeforeEach
    void setUp() {
        manager = new ModelConfigManager();
        ReflectionTestUtils.setField(manager, "defaultModel", "deepseek-v3.2");
        ReflectionTestUtils.setField(manager, "defaultTemperature", 0.7);
        ReflectionTestUtils.setField(manager, "defaultMaxTokens", 2048);
        ReflectionTestUtils.setField(manager, "defaultTopP", 1.0);
        manager.init();
    }

    @Test
    void testGetDefaultConfig() {
        ModelConfigManager.ModelConfig config = manager.getConfig("default");
        assertNotNull(config);
        assertEquals("deepseek-v3.2", config.model());
        assertEquals(0.7, config.temperature());
        assertEquals(2048, config.maxTokens());
        assertEquals(1.0, config.topP());
        assertEquals(1L, config.version());
        assertTrue(config.updatedAt() > 0);
    }

    @Test
    void testGetConfigNullReturnsDefault() {
        ModelConfigManager.ModelConfig config = manager.getConfig(null);
        assertNotNull(config);
        assertEquals("deepseek-v3.2", config.model());
    }

    @Test
    void testGetConfigBlankReturnsDefault() {
        ModelConfigManager.ModelConfig config = manager.getConfig("");
        assertNotNull(config);
        assertEquals("deepseek-v3.2", config.model());
    }

    @Test
    void testGetConfigUnknownReturnsDefault() {
        ModelConfigManager.ModelConfig config = manager.getConfig("nonexistent");
        assertNotNull(config);
        assertEquals("deepseek-v3.2", config.model());
    }

    @Test
    void testUpdateConfig() {
        ModelConfigManager.ModelConfig original = manager.getConfig("default");
        assertEquals(1L, original.version());

        ModelConfigManager.ModelConfig newConfig = new ModelConfigManager.ModelConfig(
                "gpt-4", 0.5, 4096, 0.9, 0L, 0L);
        ModelConfigManager.ModelConfig updated = manager.updateConfig("default", newConfig);

        assertEquals("gpt-4", updated.model());
        assertEquals(0.5, updated.temperature());
        assertEquals(4096, updated.maxTokens());
        assertEquals(0.9, updated.topP());
        assertEquals(2L, updated.version()); // version + 1
        assertTrue(updated.updatedAt() >= original.updatedAt());
    }

    @Test
    void testUpdateConfigMultipleTimes() {
        ModelConfigManager.ModelConfig cfg = new ModelConfigManager.ModelConfig(
                "m1", 0.1, 100, 0.5, 0L, 0L);

        ModelConfigManager.ModelConfig v1 = manager.updateConfig("default", cfg);
        assertEquals(2L, v1.version());

        ModelConfigManager.ModelConfig v2 = manager.updateConfig("default", cfg);
        assertEquals(3L, v2.version());

        ModelConfigManager.ModelConfig v3 = manager.updateConfig("default", cfg);
        assertEquals(4L, v3.version());
    }

    @Test
    void testUpdateConfigNewKey() {
        ModelConfigManager.ModelConfig newConfig = new ModelConfigManager.ModelConfig(
                "claude-3", 0.3, 8192, 0.95, 0L, 0L);
        ModelConfigManager.ModelConfig updated = manager.updateConfig("custom", newConfig);

        assertEquals("claude-3", updated.model());
        assertEquals(1L, updated.version()); // 新 key 从 version=1 开始
    }

    @Test
    void testUpdateConfigNullKeyUsesDefault() {
        ModelConfigManager.ModelConfig newConfig = new ModelConfigManager.ModelConfig(
                "gpt-4", 0.5, 4096, 0.9, 0L, 0L);
        ModelConfigManager.ModelConfig updated = manager.updateConfig(null, newConfig);

        assertEquals("gpt-4", updated.model());
        assertEquals(2L, updated.version());

        // 验证 default key 被更新
        assertEquals("gpt-4", manager.getConfig("default").model());
    }

    @Test
    void testListConfigs() {
        Map<String, ModelConfigManager.ModelConfig> configs = manager.listConfigs();
        assertNotNull(configs);
        assertTrue(configs.containsKey("default"));
        assertEquals(1, configs.size());
    }

    @Test
    void testListConfigsAfterUpdate() {
        manager.updateConfig("custom", new ModelConfigManager.ModelConfig(
                "gpt-4", 0.5, 4096, 0.9, 0L, 0L));
        Map<String, ModelConfigManager.ModelConfig> configs = manager.listConfigs();

        assertEquals(2, configs.size());
        assertTrue(configs.containsKey("default"));
        assertTrue(configs.containsKey("custom"));
    }

    @Test
    void testListConfigsIsImmutable() {
        Map<String, ModelConfigManager.ModelConfig> configs = manager.listConfigs();
        assertThrows(UnsupportedOperationException.class, () ->
                configs.put("hack", new ModelConfigManager.ModelConfig("x", 0, 0, 0, 0L, 0L)));
    }
}
