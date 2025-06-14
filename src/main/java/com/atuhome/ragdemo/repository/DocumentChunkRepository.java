package com.atuhome.ragdemo.repository;

import com.atuhome.ragdemo.model.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    
    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);
    
    long countByDocumentId(UUID documentId);
    
    @Query(value = """
        SELECT c FROM DocumentChunk c 
        WHERE c.embedding IS NOT NULL
        ORDER BY c.id
        """)
    List<DocumentChunk> findAllWithEmbeddings();
    
    @Modifying
    @Query(value = "UPDATE document_chunks SET embedding = :embedding::vector WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);
    
    @Query("SELECT COUNT(c) FROM DocumentChunk c WHERE c.embedding IS NULL")
    long countChunksWithoutEmbedding();
    
    @Query("SELECT c FROM DocumentChunk c WHERE c.embedding IS NULL ORDER BY c.createdAt")
    List<DocumentChunk> findChunksWithoutEmbedding();
    
    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}