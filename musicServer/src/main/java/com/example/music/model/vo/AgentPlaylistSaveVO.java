package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentPlaylistSaveVO {
    private Long playlistId;
    private String title;
    private Integer songCount;
}

