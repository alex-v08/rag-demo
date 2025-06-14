package com.atuhome.ragdemo.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    
    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private Integer chunkIndex;
    private String content;
    private Double similarity;
    private Integer pageNumber;
    private Integer charStart;
    private Integer charEnd;
}