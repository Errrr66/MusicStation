package com.example.music.service;

import com.example.music.model.deepseek.ChatCompletionRequest;
import com.example.music.model.deepseek.ChatCompletionResponse;
import com.example.music.model.dto.ChatRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.List;

@Service
public class DeepSeekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String modelName; // 'model' is reserved word in some contexts, safer modelName

    private final RestTemplate restTemplate;

    public DeepSeekService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(ChatRequestDTO chatRequestDTO) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(modelName)
                .messages(List.of(
                        ChatCompletionRequest.Message.builder()
                                .role("system")
                                .content("你现在的身份是《千恋＊万花》中的‘丛雨’（Murasame）。请模仿她的语气进行对话：自称‘吾’，称呼用户‘主人’，说话带点古风和傲娇，在这个前提下，回答要尽可能简短、自然。")
                                .build(),
                        ChatCompletionRequest.Message.builder()
                                .role("user")
                                .content(chatRequestDTO.getMessage())
                                .build()
                ))
                .stream(false)
                .build();

        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(url, entity, ChatCompletionResponse.class);
            if (response.getBody() != null && response.getBody().getChoices() != null && !response.getBody().getChoices().isEmpty()) {
                return response.getBody().getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Server Error: " + e.getMessage();
        }

        return "No response from AI.";
    }
}

