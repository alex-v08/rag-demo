package com.atuhome.ragdemo.service.config;

import com.atuhome.ragdemo.service.ai.AntiHallucinationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio para gestionar la configuración de sectores en el sistema RAG.
 * Permite establecer y recuperar configuraciones específicas por usuario u organización.
 */
@Service
public class SectorConfigurationService {
    
    private static final Logger log = LoggerFactory.getLogger(SectorConfigurationService.class);
    
    private final AntiHallucinationFactory antiHallucinationFactory;
    
    // Configuración global por defecto
    private volatile String defaultSector = "default";
    
    // Configuraciones por sesión/usuario (en memoria para esta implementación)
    private final ConcurrentMap<String, SectorConfiguration> sessionConfigurations = new ConcurrentHashMap<>();
    
    // Configuraciones por organización
    private final ConcurrentMap<String, SectorConfiguration> organizationConfigurations = new ConcurrentHashMap<>();
    
    public SectorConfigurationService(AntiHallucinationFactory antiHallucinationFactory) {
        this.antiHallucinationFactory = antiHallucinationFactory;
        log.info("SectorConfigurationService inicializado con sector por defecto: {}", defaultSector);
    }
    
    /**
     * Establece el sector por defecto del sistema.
     * 
     * @param sector El sector a establecer como defecto
     * @return true si el sector es válido y se estableció correctamente
     */
    public boolean setDefaultSector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            log.warn("Intento de establecer sector por defecto nulo o vacío");
            return false;
        }
        
        String normalizedSector = sector.toLowerCase().trim();
        
        if (!antiHallucinationFactory.hasSectorService(normalizedSector) && !normalizedSector.equals("default")) {
            log.warn("Intento de establecer sector no válido como defecto: {}", normalizedSector);
            return false;
        }
        
        String previousSector = this.defaultSector;
        this.defaultSector = normalizedSector;
        
        log.info("Sector por defecto cambiado de '{}' a '{}'", previousSector, normalizedSector);
        return true;
    }
    
    /**
     * Obtiene el sector por defecto del sistema.
     * 
     * @return El sector por defecto
     */
    public String getDefaultSector() {
        return defaultSector;
    }
    
    /**
     * Establece la configuración de sector para una sesión específica.
     * 
     * @param sessionId Identificador de sesión
     * @param sector Sector a configurar
     * @param customSettings Configuraciones adicionales
     * @return true si se estableció correctamente
     */
    public boolean setSessionSector(String sessionId, String sector, SectorSettings customSettings) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("Session ID nulo o vacío");
            return false;
        }
        
        if (sector == null || sector.trim().isEmpty()) {
            log.warn("Sector nulo o vacío para sesión: {}", sessionId);
            return false;
        }
        
        String normalizedSector = sector.toLowerCase().trim();
        
        if (!antiHallucinationFactory.hasSectorService(normalizedSector) && !normalizedSector.equals("default")) {
            log.warn("Sector no válido para sesión {}: {}", sessionId, normalizedSector);
            return false;
        }
        
        SectorConfiguration config = new SectorConfiguration(
            normalizedSector,
            customSettings != null ? customSettings : new SectorSettings(),
            System.currentTimeMillis()
        );
        
        sessionConfigurations.put(sessionId, config);
        log.info("Configuración de sector establecida para sesión {}: {}", sessionId, normalizedSector);
        
        return true;
    }
    
    /**
     * Obtiene la configuración de sector para una sesión específica.
     * 
     * @param sessionId Identificador de sesión
     * @return La configuración de sector o null si no existe
     */
    public SectorConfiguration getSessionConfiguration(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        
        return sessionConfigurations.get(sessionId);
    }
    
    /**
     * Obtiene el sector efectivo para una sesión (sesión > organización > defecto).
     * 
     * @param sessionId Identificador de sesión
     * @param organizationId Identificador de organización (opcional)
     * @return El sector a utilizar
     */
    public String getEffectiveSector(String sessionId, String organizationId) {
        // 1. Verificar configuración de sesión
        if (sessionId != null) {
            SectorConfiguration sessionConfig = sessionConfigurations.get(sessionId);
            if (sessionConfig != null) {
                log.debug("Usando sector de sesión para {}: {}", sessionId, sessionConfig.getSector());
                return sessionConfig.getSector();
            }
        }
        
        // 2. Verificar configuración de organización
        if (organizationId != null) {
            SectorConfiguration orgConfig = organizationConfigurations.get(organizationId);
            if (orgConfig != null) {
                log.debug("Usando sector de organización para {}: {}", organizationId, orgConfig.getSector());
                return orgConfig.getSector();
            }
        }
        
        // 3. Usar configuración por defecto
        log.debug("Usando sector por defecto: {}", defaultSector);
        return defaultSector;
    }
    
    /**
     * Establece configuración para una organización.
     * 
     * @param organizationId Identificador de organización
     * @param sector Sector a configurar
     * @param settings Configuraciones adicionales
     * @return true si se estableció correctamente
     */
    public boolean setOrganizationSector(String organizationId, String sector, SectorSettings settings) {
        if (organizationId == null || organizationId.trim().isEmpty()) {
            log.warn("Organization ID nulo o vacío");
            return false;
        }
        
        if (sector == null || sector.trim().isEmpty()) {
            log.warn("Sector nulo o vacío para organización: {}", organizationId);
            return false;
        }
        
        String normalizedSector = sector.toLowerCase().trim();
        
        if (!antiHallucinationFactory.hasSectorService(normalizedSector) && !normalizedSector.equals("default")) {
            log.warn("Sector no válido para organización {}: {}", organizationId, normalizedSector);
            return false;
        }
        
        SectorConfiguration config = new SectorConfiguration(
            normalizedSector,
            settings != null ? settings : new SectorSettings(),
            System.currentTimeMillis()
        );
        
        organizationConfigurations.put(organizationId, config);
        log.info("Configuración de sector establecida para organización {}: {}", organizationId, normalizedSector);
        
        return true;
    }
    
    /**
     * Obtiene todos los sectores disponibles en el sistema.
     * 
     * @return Lista de sectores disponibles
     */
    public List<String> getAvailableSectors() {
        return antiHallucinationFactory.getAvailableSectors();
    }
    
    /**
     * Obtiene información sobre el servicio de un sector.
     * 
     * @param sector El sector a consultar
     * @return Información del servicio
     */
    public AntiHallucinationFactory.ServiceInfo getSectorServiceInfo(String sector) {
        return antiHallucinationFactory.getServiceInfo(sector);
    }
    
    /**
     * Limpia las configuraciones de sesión expiradas.
     * 
     * @param maxAgeMs Edad máxima en milisegundos
     * @return Número de configuraciones eliminadas
     */
    public int cleanupExpiredSessions(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        int removed = 0;
        
        sessionConfigurations.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue().getTimestamp()) > maxAgeMs;
            if (expired) {
                log.debug("Limpiando configuración expirada de sesión: {}", entry.getKey());
                return true;
            }
            return false;
        });
        
        if (removed > 0) {
            log.info("Limpiadas {} configuraciones de sesión expiradas", removed);
        }
        
        return removed;
    }
    
    /**
     * Configuración de sector con metadatos.
     */
    public static class SectorConfiguration {
        private final String sector;
        private final SectorSettings settings;
        private final long timestamp;
        
        public SectorConfiguration(String sector, SectorSettings settings, long timestamp) {
            this.sector = sector;
            this.settings = settings;
            this.timestamp = timestamp;
        }
        
        public String getSector() { return sector; }
        public SectorSettings getSettings() { return settings; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Configuraciones adicionales por sector.
     */
    public static class SectorSettings {
        private double similarityThreshold = 0.2;
        private int maxResults = 5;
        private boolean strictValidation = true;
        private String customPromptId;
        
        public SectorSettings() {}
        
        public SectorSettings(double similarityThreshold, int maxResults, boolean strictValidation, String customPromptId) {
            this.similarityThreshold = similarityThreshold;
            this.maxResults = maxResults;
            this.strictValidation = strictValidation;
            this.customPromptId = customPromptId;
        }
        
        // Getters y setters
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public boolean isStrictValidation() { return strictValidation; }
        public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }
        
        public String getCustomPromptId() { return customPromptId; }
        public void setCustomPromptId(String customPromptId) { this.customPromptId = customPromptId; }
    }
}