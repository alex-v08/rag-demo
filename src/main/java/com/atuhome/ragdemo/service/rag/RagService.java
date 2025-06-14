package com.atuhome.ragdemo.service.rag;

import com.atuhome.ragdemo.exception.RagException;
import com.atuhome.ragdemo.model.dto.response.AnswerResponse;
import com.atuhome.ragdemo.model.dto.response.SearchResult;
import com.atuhome.ragdemo.model.entity.QAHistory;
import com.atuhome.ragdemo.repository.QAHistoryRepository;
import com.atuhome.ragdemo.service.ai.AntiHallucinationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final SemanticSearchService semanticSearchService;
    private final ContextBuilderService contextBuilderService;
    private final AntiHallucinationService antiHallucinationService;
    private final ChatClient chatClient;
    private final QAHistoryRepository qaHistoryRepository;

    @Value("${spring.ai.ollama.chat.options.model:deepseek-r1:latest}")
    private String modelName;

    @Transactional
    public AnswerResponse processQuestion(String question) {
        log.info("Procesando pregunta: {}", question);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Verificar que hay documentos indexados
            if (!semanticSearchService.hasIndexedDocuments()) {
                return createNoDocumentsResponse(question, startTime);
            }
            
            // 2. Búsqueda semántica
            List<SearchResult> searchResults = semanticSearchService.findSimilarChunks(question);
            
            if (searchResults.isEmpty()) {
                return createNoResultsResponse(question, startTime);
            }
            
            log.debug("Encontrados {} chunks relevantes", searchResults.size());
            
            // 3. Construir contexto
            String context = contextBuilderService.buildContext(searchResults);
            
            // 4. Crear prompt con anti-alucinación
            String prompt = antiHallucinationService.createStrictPrompt(question, context);
            
            // 5. Generar respuesta con LLM
            String answer = generateAnswer(prompt);
            
            // 6. Validar respuesta
            if (!antiHallucinationService.validateResponse(answer)) {
                log.warn("Respuesta falló validación, usando respuesta de fallback");
                answer = antiHallucinationService.createFallbackResponse(question);
            }
            
            // 7. Calcular tiempo de respuesta
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 8. Crear respuesta
            AnswerResponse response = AnswerResponse.builder()
                    .question(question)
                    .answer(answer)
                    .sources(searchResults)
                    .responseTimeMs(responseTime)
                    .timestamp(LocalDateTime.now())
                    .modelUsed(modelName)
                    .build();
            
            // 9. Guardar en historial
            saveToHistory(response, context);
            
            log.info("Pregunta procesada exitosamente en {}ms", responseTime);
            return response;
            
        } catch (Exception e) {
            log.error("Error procesando pregunta: {}", question, e);
            long responseTime = System.currentTimeMillis() - startTime;
            return createErrorResponse(question, e.getMessage(), responseTime);
        }
    }

    public AnswerResponse processQuestionWithCustomParams(String question, 
                                                         double similarityThreshold, 
                                                         int maxResults) {
        log.info("Procesando pregunta con parámetros personalizados: {} (threshold: {}, max: {})", 
                question, similarityThreshold, maxResults);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Búsqueda con parámetros personalizados
            List<SearchResult> searchResults = semanticSearchService.findSimilarChunks(
                question, similarityThreshold, maxResults);
            
            if (searchResults.isEmpty()) {
                return createNoResultsResponse(question, startTime);
            }
            
            // Continuar con el procesamiento normal
            String context = contextBuilderService.buildContext(searchResults);
            String prompt = antiHallucinationService.createStrictPrompt(question, context);
            String answer = generateAnswer(prompt);
            
            if (!antiHallucinationService.validateResponse(answer)) {
                answer = antiHallucinationService.createFallbackResponse(question);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            AnswerResponse response = AnswerResponse.builder()
                    .question(question)
                    .answer(answer)
                    .sources(searchResults)
                    .responseTimeMs(responseTime)
                    .timestamp(LocalDateTime.now())
                    .modelUsed(modelName)
                    .build();
            
            saveToHistory(response, context);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error procesando pregunta con parámetros personalizados", e);
            long responseTime = System.currentTimeMillis() - startTime;
            return createErrorResponse(question, e.getMessage(), responseTime);
        }
    }

    private String generateAnswer(String prompt) {
        try {
            log.debug("Generando respuesta con LLM");
            
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            if (answer == null || answer.trim().isEmpty()) {
                throw new RagException("El modelo no generó una respuesta");
            }
            
            log.debug("Respuesta generada: {} caracteres", answer.length());
            return answer.trim();
            
        } catch (Exception e) {
            log.error("Error generando respuesta con LLM", e);
            throw new RagException("Error al generar respuesta", e);
        }
    }

    @Transactional
    private void saveToHistory(AnswerResponse response, String context) {
        try {
            // Crear mapa de sources
            Map<String, Object> sourcesMap = new HashMap<>();
            sourcesMap.put("chunks", response.getSources());
            sourcesMap.put("total_chunks", response.getSources().size());
            
            QAHistory history = QAHistory.builder()
                    .question(response.getQuestion())
                    .answer(response.getAnswer())
                    .contextUsed(context)
                    .sources(sourcesMap)
                    .modelUsed(response.getModelUsed())
                    .responseTimeMs(response.getResponseTimeMs().intValue())
                    .build();
            
            qaHistoryRepository.save(history);
            log.debug("Interacción guardada en historial");
            
        } catch (Exception e) {
            log.error("Error guardando en historial Q&A", e);
            // No propagamos el error para no afectar la respuesta principal
        }
    }

    private AnswerResponse createNoDocumentsResponse(String question, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        
        return AnswerResponse.builder()
                .question(question)
                .answer("No hay documentos cargados en el sistema. Por favor, carga algunos documentos PDF primero.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelName)
                .build();
    }

    private AnswerResponse createNoResultsResponse(String question, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        
        return AnswerResponse.builder()
                .question(question)
                .answer("No encontré información relevante sobre esta pregunta en los documentos disponibles. " +
                       "Intenta reformular la pregunta o verifica que los documentos relacionados estén cargados.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelName)
                .build();
    }

    private AnswerResponse createErrorResponse(String question, String errorMessage, long responseTime) {
        return AnswerResponse.builder()
                .question(question)
                .answer("Lo siento, ocurrió un error procesando tu pregunta. Por favor, intenta nuevamente.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelName)
                .build();
    }

    public boolean isSystemReady() {
        try {
            return semanticSearchService.hasIndexedDocuments() && 
                   semanticSearchService.getPendingEmbeddingsCount() == 0;
        } catch (Exception e) {
            log.error("Error verificando estado del sistema", e);
            return false;
        }
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("indexed_chunks", semanticSearchService.getIndexedChunksCount());
            stats.put("pending_embeddings", semanticSearchService.getPendingEmbeddingsCount());
            stats.put("total_questions", qaHistoryRepository.count());
            stats.put("average_response_time", qaHistoryRepository.getAverageResponseTime());
            stats.put("system_ready", isSystemReady());
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas del sistema", e);
            stats.put("error", "Error obteniendo estadísticas");
        }
        
        return stats;
    }
}