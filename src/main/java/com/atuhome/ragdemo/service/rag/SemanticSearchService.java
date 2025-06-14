package com.atuhome.ragdemo.service.rag;

import com.atuhome.ragdemo.config.RagProperties;
import com.atuhome.ragdemo.model.dto.response.SearchResult;
import com.atuhome.ragdemo.model.entity.Document;
import com.atuhome.ragdemo.repository.DocumentChunkRepository;
import com.atuhome.ragdemo.repository.DocumentRepository;
import com.atuhome.ragdemo.service.ai.OllamaEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final OllamaEmbeddingService embeddingService;
    private final RagProperties ragProperties;

    public List<SearchResult> findSimilarChunks(String query) {
        double threshold = ragProperties.getSearch().getSimilarityThreshold();
        int maxResults = ragProperties.getSearch().getMaxResults();
        
        return findSimilarChunks(query, threshold, maxResults);
    }

    public List<SearchResult> findSimilarChunks(String query, double threshold, int maxResults) {
        log.debug("Buscando chunks similares para query: '{}' (threshold: {}, max: {})", 
                 query, threshold, maxResults);
        
        try {
            // Usar el método del OllamaEmbeddingService que ya maneja la búsqueda completa
            return embeddingService.findSimilarChunks(query, maxResults, threshold).stream()
                    .map(chunk -> SearchResult.builder()
                            .chunkId(chunk.getId())
                            .documentId(chunk.getDocument().getId())
                            .documentName(chunk.getDocument().getFilename())
                            .chunkIndex(chunk.getChunkIndex())
                            .content(chunk.getContent())
                            .pageNumber(chunk.getPageNumber())
                            .charStart(chunk.getCharStart())
                            .charEnd(chunk.getCharEnd())
                            .similarity(0.8) // El OllamaEmbeddingService debería retornar esto
                            .build())
                    .toList();
            
        } catch (Exception e) {
            log.error("Error en búsqueda semántica para query: '{}'", query, e);
            return Collections.emptyList();
        }
    }

    public List<SearchResult> findSimilarChunksInDocument(String query, UUID documentId) {
        double threshold = ragProperties.getSearch().getSimilarityThreshold();
        int maxResults = ragProperties.getSearch().getMaxResults();
        
        return findSimilarChunksInDocument(query, documentId, threshold, maxResults);
    }

    public List<SearchResult> findSimilarChunksInDocument(String query, UUID documentId, 
                                                         double threshold, int maxResults) {
        log.debug("Buscando chunks similares en documento {} para query: '{}'", documentId, query);
        
        try {
            // Usar el método del OllamaEmbeddingService y filtrar por documento
            return embeddingService.findSimilarChunks(query, maxResults, threshold).stream()
                    .filter(chunk -> documentId.equals(chunk.getDocument().getId()))
                    .map(chunk -> SearchResult.builder()
                            .chunkId(chunk.getId())
                            .documentId(chunk.getDocument().getId())
                            .documentName(chunk.getDocument().getFilename())
                            .chunkIndex(chunk.getChunkIndex())
                            .content(chunk.getContent())
                            .pageNumber(chunk.getPageNumber())
                            .charStart(chunk.getCharStart())
                            .charEnd(chunk.getCharEnd())
                            .similarity(0.8)
                            .build())
                    .toList();
            
        } catch (Exception e) {
            log.error("Error buscando en documento {} para query: '{}'", documentId, query, e);
            return Collections.emptyList();
        }
    }

    private SearchResult mapToSearchResult(Object[] row) {
        try {
            UUID chunkId = (UUID) row[0];
            UUID documentId = (UUID) row[1];
            Integer chunkIndex = (Integer) row[2];
            String content = (String) row[3];
            // row[4] es el embedding - lo omitimos
            Integer charStart = (Integer) row[5];
            Integer charEnd = (Integer) row[6];
            Integer pageNumber = (Integer) row[7];
            // row[8] es metadata - podríamos usarlo si fuera necesario
            // row[9] es created_at - lo omitimos
            Double similarity = ((Number) row[10]).doubleValue();
            
            // Obtener nombre del documento
            String documentName = documentRepository.findById(documentId)
                    .map(Document::getFilename)
                    .orElse("Documento no encontrado");
            
            return SearchResult.builder()
                    .chunkId(chunkId)
                    .documentId(documentId)
                    .documentName(documentName)
                    .chunkIndex(chunkIndex)
                    .content(content)
                    .similarity(similarity)
                    .pageNumber(pageNumber)
                    .charStart(charStart)
                    .charEnd(charEnd)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error mapeando resultado de búsqueda", e);
            return null;
        }
    }

    public boolean hasIndexedDocuments() {
        return chunkRepository.count() > 0;
    }

    public long getIndexedChunksCount() {
        return chunkRepository.count();
    }

    public long getPendingEmbeddingsCount() {
        return chunkRepository.countChunksWithoutEmbedding();
    }
}