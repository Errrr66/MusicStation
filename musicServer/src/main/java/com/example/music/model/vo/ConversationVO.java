package com.example.music.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long friendUserId;

    private String friendUsername;

    private String friendAvatar;

    private String lastMessage;

    private String lastMessageType;

    private Integer unreadCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

