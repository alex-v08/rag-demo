package com.atuhome.ragdemo.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request para configurar sector y parámetros específicos.
 */
@Schema(description = "Configuración de sector para el sistema RAG")
public class SectorConfigRequest {
    
    @Schema(description = "Sector a configurar", example = "legal", 
            allowableValues = {"legal", "medical", "education", "sales", "default"})
    @NotBlank(message = "El sector es obligatorio")
    private String sector;
    
    @Schema(description = "Umbral de similitud para búsqueda semántica", example = "0.2")
    @DecimalMin(value = "0.0", message = "El umbral de similitud debe ser mayor o igual a 0.0")
    @DecimalMax(value = "1.0", message = "El umbral de similitud debe ser menor o igual a 1.0")
    private Double similarityThreshold;
    
    @Schema(description = "Número máximo de resultados a devolver", example = "5")
    @Min(value = 1, message = "El máximo de resultados debe ser al menos 1")
    @Max(value = 20, message = "El máximo de resultados no puede ser mayor a 20")
    private Integer maxResults;
    
    @Schema(description = "Activar validación estricta de respuestas", example = "true")
    private Boolean strictValidation;
    
    @Schema(description = "ID de prompt personalizado (opcional)", example = "custom_legal_prompt_v1")
    private String customPromptId;
    
    @Schema(description = "Identificador de sesión (opcional)", example = "session_12345")
    private String sessionId;
    
    @Schema(description = "Identificador de organización (opcional)", example = "org_legal_firm")
    private String organizationId;
    
    // Constructores
    public SectorConfigRequest() {}
    
    public SectorConfigRequest(String sector) {
        this.sector = sector;
    }
    
    // Getters y Setters
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
    }
    
    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    
    public Boolean getStrictValidation() {
        return strictValidation;
    }
    
    public void setStrictValidation(Boolean strictValidation) {
        this.strictValidation = strictValidation;
    }
    
    public String getCustomPromptId() {
        return customPromptId;
    }
    
    public void setCustomPromptId(String customPromptId) {
        this.customPromptId = customPromptId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
    
    @Override
    public String toString() {
        return "SectorConfigRequest{" +
                "sector='" + sector + '\'' +
                ", similarityThreshold=" + similarityThreshold +
                ", maxResults=" + maxResults +
                ", strictValidation=" + strictValidation +
                ", customPromptId='" + customPromptId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", organizationId='" + organizationId + '\'' +
                '}';
    }
}