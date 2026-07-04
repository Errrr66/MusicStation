package com.example.music.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrivateMessageDTO {

    @NotNull(message = "接收人不能为空")
    private Long toUserId;

    private String messageType;

    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String content;

    private Long songId;

    private Long playlistId;
}

