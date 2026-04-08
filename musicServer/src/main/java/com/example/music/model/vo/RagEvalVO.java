package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagEvalVO {
    private int caseCount;
    private int topK;
    // Backward-compatible: strict metrics remain in these fields.
    private double recallAtK;
    private double mrr;
    private double hitRate;
    private double softRecallAtK;
    private double softMrr;
    private double softHitRate;
    private List<CaseResultVO> details;

    @Data
    @Builder
    public static class CaseResultVO {
        private String query;
        private String intent;
        private boolean hit;
        private boolean softHit;
        private double recall;
        private double softRecall;
        private double reciprocalRank;
        private double softReciprocalRank;
        private List<AgentChatResponseVO.CitationVO> retrieved;
    }
}

