package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagDebugRetrieveVO {
    private String query;
    private String intent;
    private boolean ragEnabled;
    private String promptContext;
    private AgentRagServiceHealthVO retrievalHealth;
    private List<AgentChatResponseVO.CitationVO> citations;

    @Data
    @Builder
    public static class AgentRagServiceHealthVO {
        private String strategy;
        private String mode;
        private int queryCount;
        private int candidateCount;
        private int citationCount;
        private boolean enabled;
        private long updatedAtEpochMs;
    }
}

