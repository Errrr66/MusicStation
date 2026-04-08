package com.example.music.model.dto;

import lombok.Data;

@Data
public class RagDebugRetrieveRequestDTO {
    private String query;
    private String intent;
    private Integer topK;
    private Boolean enableRag;
}

