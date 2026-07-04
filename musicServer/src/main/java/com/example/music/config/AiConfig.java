package com.example.music.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 构建 {@link ChatClient} Bean，供统一 AI 服务使用。
 * Spring AI 自动配置已提供 {@link ChatModel} Bean（OpenAI 兼容实现，指向 DashScope）。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
