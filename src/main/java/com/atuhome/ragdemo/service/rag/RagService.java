package com.atuhome.ragdemo.service.rag;

import com.atuhome.ragdemo.exception.RagException;
import com.atuhome.ragdemo.model.dto.response.AnswerResponse;
import com.atuhome.ragdemo.model.dto.response.SearchResult;
import com.atuhome.ragdemo.model.entity.QAHistory;
import com.atuhome.ragdemo.repository.QAHistoryRepository;
import com.atuhome.ragdemo.service.ai.AntiHallucinationService;
import com.atuhome.ragdemo.service.ai.AntiHallucinationFactory;
import com.atuhome.ragdemo.service.config.SectorConfigurationService;
import com.atuhome.ragdemo.service.ai.DynamicChatService;
import com.atuhome.ragdemo.service.ai.ModelManagementService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AntiHallucinationFactory antiHallucinationFactory;
    private final SectorConfigurationService sectorConfigurationService;
    private final DynamicChatService dynamicChatService;
    private final ModelManagementService modelManagementService;
    private final QAHistoryRepository qaHistoryRepository;

    @Transactional
    public AnswerResponse processQuestion(String question) {
        return processQuestion(question, null, null);
    }
    
    @Transactional
    public AnswerResponse processQuestion(String question, String sessionId, String organizationId) {
        log.info("Procesando pregunta: {} (sesión: {}, organización: {})", question, sessionId, organizationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Obtener configuración y servicio anti-alucinación para el sector
            String effectiveSector = sectorConfigurationService.getEffectiveSector(sessionId, organizationId);
            AntiHallucinationService antiHallucinationService = antiHallucinationFactory.getService(effectiveSector);
            SectorConfigurationService.SectorConfiguration sectorConfig = 
                sectorConfigurationService.getSessionConfiguration(sessionId);
            
            log.debug("Usando sector: {} con servicio: {}", effectiveSector, antiHallucinationService.getClass().getSimpleName());
            
            // 2. Verificar que hay documentos indexados
            if (!semanticSearchService.hasIndexedDocuments()) {
                return createNoDocumentsResponse(question, startTime, effectiveSector);
            }
            
            // 3. Búsqueda semántica con parámetros configurables
            List<SearchResult> searchResults;
            if (sectorConfig != null) {
                // Usar parámetros específicos de la configuración de sector
                searchResults = semanticSearchService.findSimilarChunks(
                    question, 
                    sectorConfig.getSettings().getSimilarityThreshold(),
                    sectorConfig.getSettings().getMaxResults()
                );
            } else {
                // Usar parámetros por defecto
                searchResults = semanticSearchService.findSimilarChunks(question);
            }
            
            if (searchResults.isEmpty()) {
                return createNoResultsResponse(question, startTime, effectiveSector);
            }
            
            log.debug("Encontrados {} chunks relevantes para sector: {}", searchResults.size(), effectiveSector);
            
            // 4. Construir contexto
            String context = contextBuilderService.buildContext(searchResults);
            
            // 5. Crear prompt especializado usando el servicio del sector
            String prompt = antiHallucinationService.createPrompt(question, context);
            
            // 6. Generar respuesta con LLM
            String answer = generateAnswer(prompt);
            
            // 7. Validar respuesta con el servicio especializado
            boolean strictValidation = sectorConfig != null ? 
                sectorConfig.getSettings().isStrictValidation() : true;
                
            if (strictValidation && !antiHallucinationService.validateResponse(answer)) {
                log.warn("Respuesta falló validación del sector {}, usando respuesta de fallback", effectiveSector);
                answer = antiHallucinationService.createFallbackResponse(question);
            }
            
            // 8. Calcular tiempo de respuesta
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 9. Crear respuesta con información del sector
            AnswerResponse response = AnswerResponse.builder()
                    .question(question)
                    .answer(answer)
                    .sources(searchResults)
                    .responseTimeMs(responseTime)
                    .timestamp(LocalDateTime.now())
                    .modelUsed(modelManagementService.getCurrentChatModel())
                    .build();
            
            // 10. Guardar en historial con información del sector
            saveToHistory(response, context, effectiveSector);
            
            log.info("Pregunta procesada exitosamente en {}ms", responseTime);
            return response;
            
        } catch (Exception e) {
            log.error("Error procesando pregunta: {}", question, e);
            long responseTime = System.currentTimeMillis() - startTime;
            String sector = sectorConfigurationService.getEffectiveSector(sessionId, organizationId);
            return createErrorResponse(question, e.getMessage(), responseTime, sector);
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
            String prompt = "INSTRUCCIONES: Analiza la información proporcionada y responde directamente la pregunta.\n\n" +
                          "INFORMACIÓN DE DOCUMENTOS:\n" + context + "\n\n" +
                          "PREGUNTA: " + question + "\n\n" +
                          "ANÁLISIS Y RESPUESTA DIRECTA:";
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
                    .modelUsed(modelManagementService.getCurrentChatModel())
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
            
            String answer = dynamicChatService.chat(prompt);
            
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
        saveToHistory(response, context, "default");
    }
    
    @Transactional
    private void saveToHistory(AnswerResponse response, String context, String sector) {
        try {
            // Crear mapa de sources con información del sector
            Map<String, Object> sourcesMap = new HashMap<>();
            sourcesMap.put("chunks", response.getSources());
            sourcesMap.put("total_chunks", response.getSources().size());
            sourcesMap.put("sector_used", sector);
            
            QAHistory history = QAHistory.builder()
                    .question(response.getQuestion())
                    .answer(response.getAnswer())
                    .contextUsed(context)
                    .sources(sourcesMap)
                    .modelUsed(response.getModelUsed())
                    .responseTimeMs(response.getResponseTimeMs().intValue())
                    .build();
            
            qaHistoryRepository.save(history);
            log.debug("Interacción guardada en historial con sector: {}", sector);
            
        } catch (Exception e) {
            log.error("Error guardando en historial Q&A", e);
            // No propagamos el error para no afectar la respuesta principal
        }
    }

    private AnswerResponse createNoDocumentsResponse(String question, long startTime) {
        return createNoDocumentsResponse(question, startTime, "default");
    }
    
    private AnswerResponse createNoDocumentsResponse(String question, long startTime, String sector) {
        long responseTime = System.currentTimeMillis() - startTime;
        
        return AnswerResponse.builder()
                .question(question)
                .answer("No hay documentos cargados en el sistema. Por favor, carga algunos documentos PDF primero.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelManagementService.getCurrentChatModel())
                .build();
    }

    private AnswerResponse createNoResultsResponse(String question, long startTime) {
        return createNoResultsResponse(question, startTime, "default");
    }
    
    private AnswerResponse createNoResultsResponse(String question, long startTime, String sector) {
        long responseTime = System.currentTimeMillis() - startTime;
        
        return AnswerResponse.builder()
                .question(question)
                .answer("No encontré información relevante sobre esta pregunta en los documentos disponibles. " +
                       "Intenta reformular la pregunta o verifica que los documentos relacionados estén cargados.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelManagementService.getCurrentChatModel())
                .build();
    }

    private AnswerResponse createErrorResponse(String question, String errorMessage, long responseTime) {
        return createErrorResponse(question, errorMessage, responseTime, "default");
    }
    
    private AnswerResponse createErrorResponse(String question, String errorMessage, long responseTime, String sector) {
        return AnswerResponse.builder()
                .question(question)
                .answer("Lo siento, ocurrió un error procesando tu pregunta. Por favor, intenta nuevamente.")
                .sources(Collections.emptyList())
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .modelUsed(modelManagementService.getCurrentChatModel())
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