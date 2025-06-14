package com.atuhome.ragdemo.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerResponse {
    
    private String question;
    private String answer;
    private List<SearchResult> sources;
    private Long responseTimeMs;
    private LocalDateTime timestamp;
    private String modelUsed;
}