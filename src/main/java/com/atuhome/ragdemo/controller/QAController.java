package com.atuhome.ragdemo.controller;

import com.atuhome.ragdemo.model.dto.request.QuestionRequest;
import com.atuhome.ragdemo.model.dto.response.AnswerResponse;
import com.atuhome.ragdemo.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Tag(name = "Question & Answer", description = "APIs para sistema de preguntas y respuestas RAG")
public class QAController {

    private static final Logger log = LoggerFactory.getLogger(QAController.class);

    private final RagService ragService;

    @PostMapping("/ask")
    @Operation(
        summary = "Realizar pregunta",
        description = "Procesa una pregunta utilizando el sistema RAG y retorna una respuesta basada en los documentos"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Pregunta procesada exitosamente",
        content = @Content(schema = @Schema(implementation = AnswerResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Pregunta inválida o error de validación")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<AnswerResponse> askQuestion(
            @Parameter(description = "Pregunta a procesar", required = true)
            @Valid @RequestBody QuestionRequest request) {
        
        log.info("Recibida pregunta: {}", request.getQuestion());
        
        AnswerResponse response = ragService.processQuestion(request.getQuestion());
        
        log.info("Pregunta procesada en {}ms con {} fuentes", 
                response.getResponseTimeMs(), response.getSources().size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ask/advanced")
    @Operation(
        summary = "Realizar pregunta con parámetros avanzados",
        description = "Procesa una pregunta con parámetros personalizados de búsqueda"
    )
    public ResponseEntity<AnswerResponse> askQuestionAdvanced(
            @Parameter(description = "Pregunta a procesar", required = true)
            @Valid @RequestBody QuestionRequest request,
            
            @Parameter(description = "Umbral de similitud mínimo", example = "0.7")
            @RequestParam(defaultValue = "0.7") double similarityThreshold,
            
            @Parameter(description = "Número máximo de resultados", example = "5")
            @RequestParam(defaultValue = "5") int maxResults) {
        
        log.info("Recibida pregunta avanzada: {} (threshold: {}, maxResults: {})", 
                request.getQuestion(), similarityThreshold, maxResults);
        
        AnswerResponse response = ragService.processQuestionWithCustomParams(
                request.getQuestion(), similarityThreshold, maxResults);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quick")
    @Operation(
        summary = "Pregunta rápida",
        description = "Permite hacer una pregunta directamente como parámetro GET para testing"
    )
    public ResponseEntity<AnswerResponse> quickQuestion(
            @Parameter(description = "Pregunta a procesar", example = "¿Cuál es el plazo para presentar la demanda?")
            @RequestParam String question) {
        
        log.info("Recibida pregunta rápida: {}", question);
        
        AnswerResponse response = ragService.processQuestion(question);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(
        summary = "Estado del sistema RAG",
        description = "Verifica si el sistema RAG está listo para procesar preguntas"
    )
    public ResponseEntity<SystemStatus> getSystemStatus() {
        log.debug("Solicitando estado del sistema RAG");
        
        boolean isReady = ragService.isSystemReady();
        Map<String, Object> stats = ragService.getSystemStats();
        
        SystemStatus status = SystemStatus.builder()
                .ready(isReady)
                .indexedChunks((Long) stats.get("indexed_chunks"))
                .pendingEmbeddings((Long) stats.get("pending_embeddings"))
                .totalQuestions((Long) stats.get("total_questions"))
                .averageResponseTime((Double) stats.get("average_response_time"))
                .build();
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Estadísticas del sistema",
        description = "Obtiene estadísticas detalladas del sistema RAG"
    )
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        log.debug("Solicitando estadísticas del sistema");
        
        Map<String, Object> stats = ragService.getSystemStats();
        return ResponseEntity.ok(stats);
    }

    public static class SystemStatus {
        private final boolean ready;
        private final Long indexedChunks;
        private final Long pendingEmbeddings;
        private final Long totalQuestions;
        private final Double averageResponseTime;

        private SystemStatus(Builder builder) {
            this.ready = builder.ready;
            this.indexedChunks = builder.indexedChunks;
            this.pendingEmbeddings = builder.pendingEmbeddings;
            this.totalQuestions = builder.totalQuestions;
            this.averageResponseTime = builder.averageResponseTime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isReady() { return ready; }
        public Long getIndexedChunks() { return indexedChunks; }
        public Long getPendingEmbeddings() { return pendingEmbeddings; }
        public Long getTotalQuestions() { return totalQuestions; }
        public Double getAverageResponseTime() { return averageResponseTime; }

        public static class Builder {
            private boolean ready;
            private Long indexedChunks;
            private Long pendingEmbeddings;
            private Long totalQuestions;
            private Double averageResponseTime;

            public Builder ready(boolean ready) {
                this.ready = ready;
                return this;
            }

            public Builder indexedChunks(Long indexedChunks) {
                this.indexedChunks = indexedChunks;
                return this;
            }

            public Builder pendingEmbeddings(Long pendingEmbeddings) {
                this.pendingEmbeddings = pendingEmbeddings;
                return this;
            }

            public Builder totalQuestions(Long totalQuestions) {
                this.totalQuestions = totalQuestions;
                return this;
            }

            public Builder averageResponseTime(Double averageResponseTime) {
                this.averageResponseTime = averageResponseTime;
                return this;
            }

            public SystemStatus build() {
                return new SystemStatus(this);
            }
        }
    }
}