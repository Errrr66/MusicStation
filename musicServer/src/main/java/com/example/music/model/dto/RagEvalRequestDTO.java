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
        private List<RelevantDocDTO> relevant;
    }

    @Data
    public static class RelevantDocDTO {
        private String sourceType;
        private String sourceId;
    }
}

