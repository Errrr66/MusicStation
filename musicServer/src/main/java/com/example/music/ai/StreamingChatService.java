package com.example.music.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 真流式对话服务
 * <p>
 * 利用 Spring AI ChatClient 的 stream() 能力，逐 token 返回，
 * 替代 ChatController 旧端点按 12 字符切片的伪流式实现。
 * 复用 {@link AiService.ChatMessage} 作为历史消息载体，避免同包内重复定义。
 */
@Service
public class StreamingChatService {

    private final ChatClient chatClient;

    public StreamingChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 流式对话，返回 Flux<String>，每个 element 是一个 token/chunk。
     *
     * @param systemPrompt 系统提示词（可为空）
     * @param userMessage  当前用户输入
     * @param history      历史对话（role: user/assistant/system，content: 文本）
     * @return 逐 token 的 Flux
     */
    public Flux<String> streamChat(String systemPrompt,
                                   String userMessage,
                                   List<AiService.ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (history != null) {
            for (AiService.ChatMessage msg : history) {
                if (msg == null || msg.content() == null || msg.content().isBlank()) {
                    continue;
                }
                String role = msg.role() == null ? "user" : msg.role().toLowerCase();
                switch (role) {
                    case "system" -> messages.add(new SystemMessage(msg.content()));
                    case "assistant" -> messages.add(new AssistantMessage(msg.content()));
                    default -> messages.add(new UserMessage(msg.content()));
                }
            }
        }

        ChatClient.ChatClientRequestSpec request = chatClient.prompt();
        if (!messages.isEmpty()) {
            request = request.messages(messages);
        }
        if (userMessage != null && !userMessage.isBlank()) {
            request = request.user(userMessage);
        }
        return request.stream().content();
    }

    /**
     * 流式对话（带 Advisor），用于 RAG 等增强场景。
     */
    public Flux<String> streamChat(String systemPrompt,
                                   String userMessage,
                                   List<AiService.ChatMessage> history,
                                   org.springframework.ai.chat.client.advisor.api.Advisor... advisors) {
        List<Message> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
        if (history != null) {
            for (AiService.ChatMessage msg : history) {
                if (msg == null || msg.content() == null || msg.content().isBlank()) {
                    continue;
                }
                String role = msg.role() == null ? "user" : msg.role().toLowerCase();
                switch (role) {
                    case "system" -> messages.add(new SystemMessage(msg.content()));
                    case "assistant" -> messages.add(new AssistantMessage(msg.content()));
                    default -> messages.add(new UserMessage(msg.content()));
                }
            }
        }

        ChatClient.ChatClientRequestSpec request = chatClient.prompt();
        if (!messages.isEmpty()) {
            request = request.messages(messages);
        }
        if (advisors != null && advisors.length > 0) {
            request = request.advisors(advisors);
        }
        if (userMessage != null && !userMessage.isBlank()) {
            request = request.user(userMessage);
        }
        return request.stream().content();
    }
}
