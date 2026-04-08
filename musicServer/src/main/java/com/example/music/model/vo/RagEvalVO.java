package com.example.music.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagEvalVO {
    private int caseCount;
    private int topK;
    private double recallAtK;
    private double mrr;
    private double hitRate;
    private List<CaseResultVO> details;

    @Data
    @Builder
    public static class CaseResultVO {
        private String query;
        private boolean hit;
        private double recall;
        private double reciprocalRank;
        private List<AgentChatResponseVO.CitationVO> retrieved;
    }
}

