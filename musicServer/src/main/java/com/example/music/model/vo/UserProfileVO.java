package com.example.music.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String userAvatar;

    private String introduction;

    private Boolean following;

    private Boolean mutualFollow;

    private Long followerCount;

    private Long followingCount;

    private List<UserSimpleVO> followers;

    private List<UserSimpleVO> followingUsers;

    private List<SongVO> favoriteSongs;

    private List<PlaylistVO> favoritePlaylists;
}

