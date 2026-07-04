package com.example.music.service;

import com.example.music.model.deepseek.ChatCompletionRequest;
import com.example.music.model.deepseek.ChatCompletionResponse;
import com.example.music.model.dto.ChatRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DeepSeekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String modelName; // 'model' is reserved word in some contexts, safer modelName

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Add ObjectMapper

    public DeepSeekService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(ChatRequestDTO chatRequestDTO) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("please-set-deepseek-api-key")) {
            return "配置错误：未设置 DEEPSEEK_API_KEY，请在环境变量中配置后重启后端。";
        }

        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<ChatCompletionRequest.Message> messages = new ArrayList<>();

        // Add system prompt: 强制 JSON 格式，包含中文回复和日文 TTS 文本
        messages.add(ChatCompletionRequest.Message.builder()
                .role("system")
                .content("你是《千恋＊万花》丛雨。理解现代知识，模仿语气回复。\n" +
                        "必须返回且只返回严格的 JSON 格式：{\"chinese\": \"中文回复\", \"japanese\": \"日文翻译\"}。\n" +
                        "要求：\n" +
                        "1. chinese: 中文回复，可包含动作描写（如“（歪头）”）。\n" +
                        "2. japanese: 对应内容的日文翻译，用于语音合成。必须去除所有动作描写（括号内容），只保留能朗读的台词。\n" +
                        "3. 即使历史记录是纯文本，你也必须返回 JSON。\n" +
                        "4. 进阶要求：如果用户让你唱歌，虽然你不能真的生成旋律，但请在 'japanese' 字段中通过添加波浪号 '～'（表示拖长音）和逗号 '、'（表示停顿）来模拟歌唱的节奏感。例如：'私～は、ここに～、いるよ～'。")
                .build());

        // Limit history to last 10 messages to save tokens
        if (chatRequestDTO.getMessages() != null && !chatRequestDTO.getMessages().isEmpty()) {
            List<ChatRequestDTO.MessageDTO> allMessages = chatRequestDTO.getMessages();
            int maxHistory = 10;
            int start = Math.max(0, allMessages.size() - maxHistory);

            for (int i = start; i < allMessages.size(); i++) {
                ChatRequestDTO.MessageDTO msg = allMessages.get(i);
                // 保持历史消息原样，不进行伪造，避免误导模型
                messages.add(ChatCompletionRequest.Message.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .build());
            }
        } else {
            messages.add(ChatCompletionRequest.Message.builder()
                    .role("user")
                    .content(chatRequestDTO.getMessage())
                    .build());
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(messages)
                .stream(false)
                // 启用 JSON 模式，确保输出稳定
                .response_format(ChatCompletionRequest.ResponseFormat.builder().type("json_object").build())
                .build();

        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(url, entity, ChatCompletionResponse.class);
            if (response.getBody() != null && response.getBody().getChoices() != null && !response.getBody().getChoices().isEmpty()) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("DeepSeek API 鉴权失败(401)：{}", e.getMessage(), e);
            return "AI 服务暂时不可用，请稍后重试";
        } catch (Exception e) {
            log.error("DeepSeek 调用异常：{}", e.getMessage(), e);
            return "AI 服务暂时不可用，请稍后重试";
        }

        return "AI 服务暂时不可用，请稍后重试";
    }

    // Removed translateToJapanese to save tokens by using single pass logic

}
