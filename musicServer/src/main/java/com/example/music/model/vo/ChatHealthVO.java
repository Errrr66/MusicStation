package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatHealthVO {
    private boolean ragEnabled;
    private String ragMode;
    private RagRetrievalVO ragLastRetrieval;
    private ProviderStatusVO providers;
    private String serverTime;

    @Data
    @Builder
    public static class RagRetrievalVO {
        private String strategy;
        private String mode;
        private int queryCount;
        private int candidateCount;
        private int citationCount;
        private boolean enabled;
        private long updatedAtEpochMs;
    }

    @Data
    @Builder
    public static class ProviderStatusVO {
        private boolean deepseekConfigured;
        private boolean ttsConfigured;
        private boolean semanticConfigured;
    }
}
