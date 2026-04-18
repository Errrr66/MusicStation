package com.example.music.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrivateMessageDTO {

    @NotNull(message = "接收人不能为空")
    private Long toUserId;

    private String messageType;

    private String content;

    private Long songId;

    private Long playlistId;
}

