package com.example.music.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PrivateMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String messageType;

    private String content;

    private Long songId;

    private String songName;

    private String songArtistName;

    private String songCoverUrl;

    private Long playlistId;

    private String playlistTitle;

    private String playlistCoverUrl;

    private String fromUsername;

    private String fromUserAvatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}


