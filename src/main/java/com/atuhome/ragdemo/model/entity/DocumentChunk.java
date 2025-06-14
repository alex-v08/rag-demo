package com.atuhome.ragdemo.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "chunk_index"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(nullable = false)
    private Integer chunkIndex;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String embedding;
    
    private Integer charStart;
    private Integer charEnd;
    private Integer pageNumber;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Helper methods for embedding conversion
    public void setEmbeddingFromFloatArray(float[] embeddingArray) {
        if (embeddingArray == null) {
            this.embedding = null;
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embeddingArray.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embeddingArray[i]);
        }
        sb.append("]");
        this.embedding = sb.toString();
    }
    
    public float[] getEmbeddingAsFloatArray() {
        if (embedding == null || embedding.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Parse the vector string format: [1.0, 2.0, 3.0]
            String value = embedding.trim();
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }
            
            if (value.isEmpty()) {
                return new float[0];
            }
            
            String[] parts = value.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing embedding string: " + embedding, e);
        }
    }
}