package com.example.music.model.deepseek;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChatCompletionRequest {
    private String model;
    private List<Message> messages;
    private boolean stream;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}

