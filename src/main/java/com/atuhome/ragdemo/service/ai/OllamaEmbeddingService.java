package com.atuhome.ragdemo.service.ai;

import com.atuhome.ragdemo.config.RagProperties;
import com.atuhome.ragdemo.model.entity.DocumentChunk;
import com.atuhome.ragdemo.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.embedding.service", havingValue = "ollama", matchIfMissing = true)
public class OllamaEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepository chunkRepository;
    private final RagProperties ragProperties;

    @Transactional
    public void generateAndStoreEmbeddings(List<DocumentChunk> chunks) {
        log.info("Generando embeddings para {} chunks usando Ollama bge-m3", chunks.size());
        
        int batchSize = ragProperties.getEmbedding().getBatchSize();
        
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, endIndex);
            
            log.debug("Procesando batch {}/{} con {} chunks", 
                    (i / batchSize) + 1, 
                    (chunks.size() + batchSize - 1) / batchSize, 
                    batch.size());
            
            processBatch(batch);
        }
        
        log.info("Embeddings generados y almacenados para {} chunks", chunks.size());
    }

    private void processBatch(List<DocumentChunk> chunks) {
        try {
            // Extraer contenidos para el batch
            List<String> contents = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .toList();
            
            // Generar embeddings usando Ollama
            EmbeddingResponse response = embeddingModel.embedForResponse(contents);
            
            // Asignar embeddings a los chunks
            for (int i = 0; i < chunks.size() && i < response.getResults().size(); i++) {
                DocumentChunk chunk = chunks.get(i);
                float[] embedding = response.getResults().get(i).getOutput();
                
                chunk.setEmbedding(embedding);
                
                log.debug("Embedding generado para chunk {}: dimensión {}", 
                        chunk.getId(), embedding.length);
            }
            
            // Guardar chunks con embeddings
            chunkRepository.saveAll(chunks);
            
            log.debug("Batch de {} chunks procesado exitosamente", chunks.size());
            
        } catch (Exception e) {
            log.error("Error generando embeddings para batch de chunks", e);
            throw new RuntimeException("Error generando embeddings", e);
        }
    }
    
    public float[] generateEmbedding(String text) {
        try {
            log.debug("Generando embedding para texto de {} caracteres", text.length());
            
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            
            if (response.getResults().isEmpty()) {
                throw new RuntimeException("No se pudo generar embedding para el texto");
            }
            
            float[] embedding = response.getResults().get(0).getOutput();
            log.debug("Embedding generado con dimensión: {}", embedding.length);
            
            return embedding;
            
        } catch (Exception e) {
            log.error("Error generando embedding para texto", e);
            throw new RuntimeException("Error generando embedding", e);
        }
    }
    
    public List<DocumentChunk> findSimilarChunks(String queryText, int maxResults, double similarityThreshold) {
        try {
            log.info("Iniciando búsqueda de chunks similares para query: '{}'", queryText);
            
            float[] queryEmbedding = generateEmbedding(queryText);
            log.debug("Query embedding generado con dimensión: {}", queryEmbedding.length);
            
            log.debug("Buscando chunks similares para query. Threshold: {}, Max results: {}", 
                    similarityThreshold, maxResults);
            
            // Get all chunks with embeddings
            List<DocumentChunk> allChunks = chunkRepository.findAllWithEmbeddings();
            
            log.info("Encontrados {} chunks con embeddings", allChunks.size());
            
            if (allChunks.isEmpty()) {
                log.warn("No se encontraron chunks con embeddings");
                return List.of();
            }
            
            // Calculate similarity for each chunk
            List<DocumentChunk> similarChunks = allChunks.stream()
                    .filter(chunk -> {
                        boolean hasEmbedding = chunk.getEmbedding() != null && chunk.getEmbedding().length > 0;
                        if (!hasEmbedding) {
                            log.warn("Chunk {} no tiene embedding", chunk.getId());
                        }
                        return hasEmbedding;
                    })
                    .map(chunk -> {
                        try {
                            float[] chunkEmbedding = chunk.getEmbedding();
                            if (chunkEmbedding == null) {
                                log.warn("Chunk {} tiene embedding null después de conversión", chunk.getId());
                                return null;
                            }
                            double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                            log.debug("Chunk {}: similaridad = {}", chunk.getId(), similarity);
                            return new ChunkWithSimilarity(chunk, similarity);
                        } catch (Exception e) {
                            log.error("Error calculando similaridad para chunk {}: {}", chunk.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(chunkSim -> chunkSim != null && chunkSim.similarity > similarityThreshold)
                    .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                    .limit(maxResults)
                    .map(chunkSim -> {
                        log.info("Chunk seleccionado: {} con similaridad: {}", 
                                chunkSim.chunk.getId(), chunkSim.similarity);
                        return chunkSim.chunk;
                    })
                    .toList();
            
            log.info("Búsqueda completada. Encontrados {} chunks similares", similarChunks.size());
            return similarChunks;
            
        } catch (Exception e) {
            log.error("Error en findSimilarChunks: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private static class ChunkWithSimilarity {
        final DocumentChunk chunk;
        final double similarity;
        
        ChunkWithSimilarity(DocumentChunk chunk, double similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }
    }
}