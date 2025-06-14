package com.atuhome.ragdemo.model.dto.response;

import com.atuhome.ragdemo.model.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    
    private UUID id;
    private String filename;
    private Long fileSize;
    private DocumentStatus status;
    private LocalDateTime uploadDate;
    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;
    private String errorMessage;
    private Map<String, Object> metadata;
    private Integer chunksCount;
    private String message;
}