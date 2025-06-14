package com.atuhome.ragdemo.service.ai;

import com.atuhome.ragdemo.config.RagProperties;
import com.atuhome.ragdemo.exception.RagException;
import com.atuhome.ragdemo.model.entity.DocumentChunk;
import com.atuhome.ragdemo.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimpleEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(SimpleEmbeddingService.class);

    private final DocumentChunkRepository chunkRepository;
    private final RagProperties ragProperties;
    private final Random random = new Random();

    public float[] generateEmbedding(String text) {
        log.debug("Generando embedding simulado para texto de {} caracteres", text.length());
        
        // Generar embedding simulado (384 dimensiones)
        int dimension = ragProperties.getEmbedding().getDimension();
        float[] embedding = new float[dimension];
        
        // Usar hash del texto como semilla para consistencia
        Random textRandom = new Random(text.hashCode());
        for (int i = 0; i < dimension; i++) {
            embedding[i] = (textRandom.nextFloat() - 0.5f) * 2.0f; // valores entre -1 y 1
        }
        
        // Normalizar el vector
        float norm = 0.0f;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        log.debug("Embedding simulado generado con dimensión: {}", embedding.length);
        return embedding;
    }

    @Transactional
    public void generateAndStoreEmbeddings(List<DocumentChunk> chunks) {
        log.info("Generando y almacenando embeddings simulados para {} chunks", chunks.size());
        
        for (DocumentChunk chunk : chunks) {
            try {
                float[] embedding = generateEmbedding(chunk.getContent());
                String embeddingString = Arrays.toString(embedding);
                chunkRepository.updateEmbedding(chunk.getId(), embeddingString);
                log.debug("Embedding simulado actualizado para chunk {}", chunk.getId());
            } catch (Exception e) {
                log.error("Error generando embedding para chunk {}", chunk.getId(), e);
                throw new RagException("Error generando embedding", e);
            }
        }
        
        log.info("Embeddings simulados generados y almacenados exitosamente");
    }

    public void processAllPendingEmbeddings() {
        log.info("Procesando todos los chunks pendientes de embedding");
        
        List<DocumentChunk> chunksWithoutEmbedding = chunkRepository.findChunksWithoutEmbedding();
        
        if (chunksWithoutEmbedding.isEmpty()) {
            log.info("No hay chunks pendientes de procesar");
            return;
        }
        
        log.info("Encontrados {} chunks sin embedding", chunksWithoutEmbedding.size());
        generateAndStoreEmbeddings(chunksWithoutEmbedding);
    }

    public long getPendingEmbeddingsCount() {
        return chunkRepository.countChunksWithoutEmbedding();
    }
}