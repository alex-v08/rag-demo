package com.atuhome.ragdemo.repository;

import com.atuhome.ragdemo.model.entity.Document;
import com.atuhome.ragdemo.model.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    List<Document> findByStatusOrderByUploadDateDesc(DocumentStatus status);
    
    Page<Document> findByStatusOrderByUploadDateDesc(DocumentStatus status, Pageable pageable);
    
    Optional<Document> findByFilename(String filename);
    
    Optional<Document> findByContentHash(String contentHash);
    
    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.processingStartedAt IS NULL")
    List<Document> findPendingDocuments(@Param("status") DocumentStatus status);
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.chunks WHERE d.id = :id")
    Optional<Document> findByIdWithChunks(@Param("id") UUID id);
    
    long countByStatus(DocumentStatus status);
}