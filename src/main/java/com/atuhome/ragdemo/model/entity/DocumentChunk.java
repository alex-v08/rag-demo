package com.atuhome.ragdemo.model.entity;

import com.atuhome.ragdemo.config.PostgreSQLVectorType;
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
    
    @Type(PostgreSQLVectorType.class)
    @Column(columnDefinition = "vector(1024)")
    private float[] embedding;
    
    private Integer charStart;
    private Integer charEnd;
    private Integer pageNumber;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
}