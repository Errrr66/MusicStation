package com.example.music.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentPlaylistSaveDTO {
    private String title;
    private String introduction;
    private String style;
    private String coverUrl;
    private List<TrackDTO> tracks;

    @Data
    public static class TrackDTO {
        private Long songId;
        private String songName;
        private String artistName;
    }
}

