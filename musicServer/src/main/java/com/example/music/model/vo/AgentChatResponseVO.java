package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgentChatResponseVO {
    private String answer;
    private String audio;
    private String intent;
    private String playerCommand;
    private List<ToolTraceVO> toolTrace;
    private List<AgentSongCardVO> songs;
    private List<AgentPlaylistCardVO> playlists;
    private Map<String, Object> musicArchive;

    @Data
    @Builder
    public static class ToolTraceVO {
        private String tool;
        private String status;
        private String summary;
    }

    @Data
    @Builder
    public static class AgentSongCardVO {
        private Long songId;
        private String songName;
        private String artistName;
        private String album;
        private String coverUrl;
        private String audioUrl;
        private String source;
        private String reason;
    }

    @Data
    @Builder
    public static class AgentPlaylistCardVO {
        private Long playlistId;
        private String title;
        private String coverUrl;
        private String source;
        private String reason;
        private Integer songCount;
        private List<AgentSongCardVO> tracks;
    }
}

