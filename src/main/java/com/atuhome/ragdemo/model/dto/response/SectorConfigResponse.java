package com.atuhome.ragdemo.model.dto.response;

import com.atuhome.ragdemo.service.ai.AntiHallucinationFactory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response con información de configuración de sector.
 */
@Schema(description = "Información de configuración de sector del sistema RAG")
public class SectorConfigResponse {
    
    @Schema(description = "Sector actualmente configurado", example = "legal")
    private String currentSector;
    
    @Schema(description = "Información del servicio activo")
    private ServiceInfo serviceInfo;
    
    @Schema(description = "Configuración efectiva aplicada")
    private ConfigurationDetails configuration;
    
    @Schema(description = "Lista de sectores disponibles")
    private List<String> availableSectors;
    
    @Schema(description = "Timestamp de la configuración")
    private LocalDateTime timestamp;
    
    @Schema(description = "Mensaje informativo")
    private String message;
    
    // Constructores
    public SectorConfigResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SectorConfigResponse(String currentSector, String message) {
        this.currentSector = currentSector;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters y Setters
    public String getCurrentSector() {
        return currentSector;
    }
    
    public void setCurrentSector(String currentSector) {
        this.currentSector = currentSector;
    }
    
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }
    
    public void setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }
    
    public ConfigurationDetails getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(ConfigurationDetails configuration) {
        this.configuration = configuration;
    }
    
    public List<String> getAvailableSectors() {
        return availableSectors;
    }
    
    public void setAvailableSectors(List<String> availableSectors) {
        this.availableSectors = availableSectors;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Clases internas
    @Schema(description = "Información del servicio anti-alucinación")
    public static class ServiceInfo {
        @Schema(description = "Sector del servicio", example = "legal")
        private String sector;
        
        @Schema(description = "Nombre de la clase del servicio", example = "LegalAntiHallucinationService")
        private String className;
        
        @Schema(description = "Indica si es un servicio especializado", example = "true")
        private boolean specialized;
        
        @Schema(description = "Descripción del servicio", example = "Servicio especializado para sector legal")
        private String description;
        
        public ServiceInfo() {}
        
        public ServiceInfo(AntiHallucinationFactory.ServiceInfo factoryInfo) {
            this.sector = factoryInfo.getSector();
            this.className = factoryInfo.getClassName();
            this.specialized = factoryInfo.isSpecialized();
            this.description = factoryInfo.getDescription();
        }
        
        // Getters y Setters
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public boolean isSpecialized() { return specialized; }
        public void setSpecialized(boolean specialized) { this.specialized = specialized; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    @Schema(description = "Detalles de configuración aplicada")
    public static class ConfigurationDetails {
        @Schema(description = "Umbral de similitud", example = "0.2")
        private double similarityThreshold;
        
        @Schema(description = "Máximo número de resultados", example = "5")
        private int maxResults;
        
        @Schema(description = "Validación estricta activada", example = "true")
        private boolean strictValidation;
        
        @Schema(description = "ID de prompt personalizado", example = "custom_legal_prompt_v1")
        private String customPromptId;
        
        @Schema(description = "Fuente de configuración", example = "session", 
                allowableValues = {"default", "organization", "session"})
        private String configurationSource;
        
        public ConfigurationDetails() {}
        
        public ConfigurationDetails(double similarityThreshold, int maxResults, 
                                  boolean strictValidation, String customPromptId, 
                                  String configurationSource) {
            this.similarityThreshold = similarityThreshold;
            this.maxResults = maxResults;
            this.strictValidation = strictValidation;
            this.customPromptId = customPromptId;
            this.configurationSource = configurationSource;
        }
        
        // Getters y Setters
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public boolean isStrictValidation() { return strictValidation; }
        public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }
        
        public String getCustomPromptId() { return customPromptId; }
        public void setCustomPromptId(String customPromptId) { this.customPromptId = customPromptId; }
        
        public String getConfigurationSource() { return configurationSource; }
        public void setConfigurationSource(String configurationSource) { this.configurationSource = configurationSource; }
    }
    
    // Builder
    public static class Builder {
        private final SectorConfigResponse response = new SectorConfigResponse();
        
        public Builder currentSector(String sector) {
            response.setCurrentSector(sector);
            return this;
        }
        
        public Builder serviceInfo(AntiHallucinationFactory.ServiceInfo info) {
            response.setServiceInfo(new ServiceInfo(info));
            return this;
        }
        
        public Builder configuration(double threshold, int maxResults, boolean strict, 
                                   String promptId, String source) {
            response.setConfiguration(new ConfigurationDetails(threshold, maxResults, strict, promptId, source));
            return this;
        }
        
        public Builder availableSectors(List<String> sectors) {
            response.setAvailableSectors(sectors);
            return this;
        }
        
        public Builder message(String message) {
            response.setMessage(message);
            return this;
        }
        
        public SectorConfigResponse build() {
            return response;
        }
    }
}