package com.example.music.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserSimpleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String userAvatar;

    private String introduction;

    private Boolean following;

    private Boolean mutualFollow;
}

