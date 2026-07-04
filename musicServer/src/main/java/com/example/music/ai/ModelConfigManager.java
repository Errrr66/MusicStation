package com.example.music.ai;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型参数动态配置管理
 * 支持运行时修改模型参数，无需重启。记录 version 和 updatedAt 实现版本控制。
 */
@Component
public class ModelConfigManager {

    @Value("${spring.ai.openai.chat.options.model:deepseek-v3.2}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private double defaultTemperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2048}")
    private int defaultMaxTokens;

    @Value("${spring.ai.openai.chat.options.top-p:1.0}")
    private double defaultTopP;

    private final Map<String, ModelConfig> configs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        long now = System.currentTimeMillis();
        configs.put("default", new ModelConfig(
                defaultModel, defaultTemperature, defaultMaxTokens, defaultTopP, 1L, now));
    }

    public record ModelConfig(
            String model,
            double temperature,
            int maxTokens,
            double topP,
            long version,
            long updatedAt
    ) {
    }

    public ModelConfig getConfig(String name) {
        if (name == null || name.isBlank()) {
            return configs.get("default");
        }
        return configs.getOrDefault(name, configs.get("default"));
    }

    public synchronized ModelConfig updateConfig(String name, ModelConfig config) {
        String key = (name == null || name.isBlank()) ? "default" : name;
        ModelConfig existing = configs.get(key);
        long newVersion = (existing == null ? 0L : existing.version()) + 1L;
        ModelConfig updated = new ModelConfig(
                config.model(),
                config.temperature(),
                config.maxTokens(),
                config.topP(),
                newVersion,
                System.currentTimeMillis());
        configs.put(key, updated);
        return updated;
    }

    public Map<String, ModelConfig> listConfigs() {
        return Map.copyOf(configs);
    }
}
