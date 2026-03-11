package com.example.music.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String message;
    private List<MessageDTO> messages;

    @Data
    public static class MessageDTO {
        private String role;
        private String content;

        public MessageDTO() {}

        public MessageDTO(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
