package com.example.music.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RagEvalRequestDTO {
    private Integer topK;
    private List<CaseDTO> cases;

    @Data
    public static class CaseDTO {
        private String query;
        private String intent;
        private List<RelevantDocDTO> relevant;
    }

    @Data
    public static class RelevantDocDTO {
        private String sourceType;
        private String sourceId;
        // Optional soft-match hint for title semantics.
        private String titleKeyword;
        // Optional soft-match hint for artist semantics.
        private String artistKeyword;
    }
}

