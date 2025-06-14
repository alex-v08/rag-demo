package com.atuhome.ragdemo.controller;

import com.atuhome.ragdemo.model.dto.response.DocumentResponse;
import com.atuhome.ragdemo.model.enums.DocumentStatus;
import com.atuhome.ragdemo.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "APIs para gestión de documentos PDF")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Cargar documento PDF",
        description = "Carga un archivo PDF al sistema para procesamiento RAG"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Documento cargado exitosamente",
        content = @Content(schema = @Schema(implementation = DocumentResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Archivo inválido o error de validación")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Parameter(description = "Archivo PDF a cargar", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("Recibida solicitud de carga de documento: {}", file.getOriginalFilename());
        
        DocumentResponse response = documentService.uploadDocument(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "Listar todos los documentos",
        description = "Obtiene la lista de todos los documentos cargados en el sistema"
    )
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        log.debug("Solicitando lista de todos los documentos");
        
        List<DocumentResponse> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/paginated")
    @Operation(
        summary = "Listar documentos con paginación",
        description = "Obtiene la lista de documentos con paginación"
    )
    public ResponseEntity<Page<DocumentResponse>> getDocumentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Solicitando documentos paginados: página {}, tamaño {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentResponse> documents = documentService.getDocuments(pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Listar documentos por estado",
        description = "Obtiene la lista de documentos filtrados por estado"
    )
    public ResponseEntity<List<DocumentResponse>> getDocumentsByStatus(
            @Parameter(description = "Estado del documento", example = "COMPLETED")
            @PathVariable DocumentStatus status) {
        
        log.debug("Solicitando documentos con estado: {}", status);
        
        List<DocumentResponse> documents = documentService.getDocumentsByStatus(status);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener documento por ID",
        description = "Obtiene los detalles de un documento específico"
    )
    @ApiResponse(responseCode = "200", description = "Documento encontrado")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @Parameter(description = "ID del documento", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        
        log.debug("Solicitando documento con ID: {}", id);
        
        DocumentResponse document = documentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar documento",
        description = "Elimina un documento del sistema junto con sus chunks y archivo"
    )
    @ApiResponse(responseCode = "204", description = "Documento eliminado exitosamente")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "ID del documento a eliminar")
            @PathVariable UUID id) {
        
        log.info("Solicitando eliminación de documento: {}", id);
        
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reprocess")
    @Operation(
        summary = "Reprocesar documento",
        description = "Marca un documento para ser reprocesado"
    )
    @ApiResponse(responseCode = "200", description = "Documento marcado para reprocesamiento")
    @ApiResponse(responseCode = "404", description = "Documento no encontrado")
    public ResponseEntity<String> reprocessDocument(
            @Parameter(description = "ID del documento a reprocesar")
            @PathVariable UUID id) {
        
        log.info("Solicitando reprocesamiento de documento: {}", id);
        
        documentService.reprocessDocument(id);
        return ResponseEntity.ok("Documento marcado para reprocesamiento");
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Obtener estadísticas de documentos",
        description = "Obtiene estadísticas generales sobre los documentos en el sistema"
    )
    public ResponseEntity<DocumentStats> getDocumentStats() {
        log.debug("Solicitando estadísticas de documentos");
        
        DocumentStats stats = DocumentStats.builder()
                .totalDocuments(documentService.getDocumentCount())
                .pendingDocuments(documentService.getDocumentCountByStatus(DocumentStatus.PENDING))
                .processingDocuments(documentService.getDocumentCountByStatus(DocumentStatus.PROCESSING))
                .completedDocuments(documentService.getDocumentCountByStatus(DocumentStatus.COMPLETED))
                .failedDocuments(documentService.getDocumentCountByStatus(DocumentStatus.FAILED))
                .build();
        
        return ResponseEntity.ok(stats);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Argumento inválido en DocumentController: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_ARGUMENT")
                .message(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    public static class DocumentStats {
        private final long totalDocuments;
        private final long pendingDocuments;
        private final long processingDocuments;
        private final long completedDocuments;
        private final long failedDocuments;

        private DocumentStats(Builder builder) {
            this.totalDocuments = builder.totalDocuments;
            this.pendingDocuments = builder.pendingDocuments;
            this.processingDocuments = builder.processingDocuments;
            this.completedDocuments = builder.completedDocuments;
            this.failedDocuments = builder.failedDocuments;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalDocuments() { return totalDocuments; }
        public long getPendingDocuments() { return pendingDocuments; }
        public long getProcessingDocuments() { return processingDocuments; }
        public long getCompletedDocuments() { return completedDocuments; }
        public long getFailedDocuments() { return failedDocuments; }

        public static class Builder {
            private long totalDocuments;
            private long pendingDocuments;
            private long processingDocuments;
            private long completedDocuments;
            private long failedDocuments;

            public Builder totalDocuments(long totalDocuments) {
                this.totalDocuments = totalDocuments;
                return this;
            }

            public Builder pendingDocuments(long pendingDocuments) {
                this.pendingDocuments = pendingDocuments;
                return this;
            }

            public Builder processingDocuments(long processingDocuments) {
                this.processingDocuments = processingDocuments;
                return this;
            }

            public Builder completedDocuments(long completedDocuments) {
                this.completedDocuments = completedDocuments;
                return this;
            }

            public Builder failedDocuments(long failedDocuments) {
                this.failedDocuments = failedDocuments;
                return this;
            }

            public DocumentStats build() {
                return new DocumentStats(this);
            }
        }
    }

    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final long timestamp;

        private ErrorResponse(Builder builder) {
            this.error = builder.error;
            this.message = builder.message;
            this.timestamp = builder.timestamp;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }

        public static class Builder {
            private String error;
            private String message;
            private long timestamp;

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder timestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }
    }
}