package com.atuhome.ragdemo.controller.advice;

import com.atuhome.ragdemo.exception.DocumentProcessingException;
import com.atuhome.ragdemo.exception.RagException;
import com.atuhome.ragdemo.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Recurso no encontrado: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("RESOURCE_NOT_FOUND")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentProcessing(DocumentProcessingException e) {
        log.error("Error procesando documento: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("DOCUMENT_PROCESSING_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RagException.class)
    public ResponseEntity<ErrorResponse> handleRagException(RagException e) {
        log.error("Error en sistema RAG: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("RAG_SYSTEM_ERROR")
                .message("Error procesando la consulta. Por favor, intenta nuevamente.")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .details(e.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
        log.warn("Error de validación: {}", e.getMessage());
        
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        Map<String, String> fieldErrors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse error = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Error de validación: " + errorMessage)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Archivo demasiado grande: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("FILE_TOO_LARGE")
                .message("El archivo excede el tamaño máximo permitido (50MB)")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Argumento inválido: {}", e.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_ARGUMENT")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Error interno no manejado: {}", e.getMessage(), e);
        
        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("Ocurrió un error interno. Por favor, contacta al administrador.")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public static class ErrorResponse {
        private final String error;
        private final String message;
        private final LocalDateTime timestamp;
        private final int status;
        private final String details;
        private final Map<String, String> fieldErrors;

        private ErrorResponse(Builder builder) {
            this.error = builder.error;
            this.message = builder.message;
            this.timestamp = builder.timestamp;
            this.status = builder.status;
            this.details = builder.details;
            this.fieldErrors = builder.fieldErrors;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getError() { return error; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getDetails() { return details; }
        public Map<String, String> getFieldErrors() { return fieldErrors; }

        public static class Builder {
            private String error;
            private String message;
            private LocalDateTime timestamp;
            private int status;
            private String details;
            private Map<String, String> fieldErrors;

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder details(String details) {
                this.details = details;
                return this;
            }

            public Builder fieldErrors(Map<String, String> fieldErrors) {
                this.fieldErrors = fieldErrors;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }
    }
}