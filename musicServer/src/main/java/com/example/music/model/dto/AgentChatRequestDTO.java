package com.example.music.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AgentChatRequestDTO {
    private String message;
    private List<ChatMessageDTO> messages;
    private NowPlayingDTO nowPlaying;
    private List<PlaylistSeedDTO> playlistSeeds;
    private Integer limit;
    private Boolean enableVoice;

    @Data
    public static class ChatMessageDTO {
        private String role;
        private String content;
    }

    @Data
    public static class NowPlayingDTO {
        private String songId;
        private String title;
        private String artist;
        private String album;
    }

    @Data
    public static class PlaylistSeedDTO {
        private Long songId;
        private String songName;
        private String artistName;
        private String style;
    }
}

