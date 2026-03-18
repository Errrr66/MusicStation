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
    private ResponseFormat response_format;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @Builder
    public static class ResponseFormat {
        private String type;
    }
}
