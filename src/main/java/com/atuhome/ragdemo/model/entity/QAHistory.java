package com.atuhome.ragdemo.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "qa_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
    
    @Column(columnDefinition = "TEXT")
    private String answer;
    
    @Column(columnDefinition = "TEXT")
    private String contextUsed;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> sources;
    
    private String modelUsed;
    private Integer responseTimeMs;
    
    @Column(name = "feedback_rating")
    private Integer feedbackRating;
    
    @Column(columnDefinition = "TEXT")
    private String feedbackComment;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}