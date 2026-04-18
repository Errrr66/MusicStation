package com.example.music.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ArtistVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 歌手 id
     */
    private Long artistId;

    /**
     * 歌手姓名
     */
    private String artistName;

    /**
     * 歌手类型：0-男歌手，1-女歌手，2-组合/乐队
     */
    private Integer gender;

    /**
     * 歌手头像
     */
    private String avatar;

    /**
     * 歌手地区
     */
    private String area;

    /**
     * 歌手简介
     */
    private String introduction;
}
