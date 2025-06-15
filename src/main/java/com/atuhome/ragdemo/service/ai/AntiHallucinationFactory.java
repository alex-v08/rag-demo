package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory para gestionar diferentes servicios de anti-alucinación por sector.
 * Permite cambiar dinámicamente entre diferentes implementaciones especializadas.
 */
@Service
public class AntiHallucinationFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AntiHallucinationFactory.class);
    
    private final ApplicationContext applicationContext;
    private final Map<String, AntiHallucinationService> serviceCache;
    private final DefaultAntiHallucinationService defaultService;
    
    public AntiHallucinationFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.serviceCache = new HashMap<>();
        this.defaultService = new DefaultAntiHallucinationService();
        
        // Inicializar servicios disponibles
        initializeServices();
    }
    
    /**
     * Obtiene el servicio anti-alucinación para un sector específico.
     * 
     * @param sector El sector (legal, medical, education, sales, etc.)
     * @return El servicio especializado para el sector, o el servicio por defecto
     */
    public AntiHallucinationService getService(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            log.debug("Sector no especificado, usando servicio por defecto");
            return defaultService;
        }
        
        String normalizedSector = sector.toLowerCase().trim();
        AntiHallucinationService service = serviceCache.get(normalizedSector);
        
        if (service != null) {
            log.debug("Usando servicio especializado para sector: {}", normalizedSector);
            return service;
        }
        
        log.warn("No se encontró servicio para sector: {}, usando servicio por defecto", normalizedSector);
        return defaultService;
    }
    
    /**
     * Obtiene todos los sectores disponibles.
     * 
     * @return Lista de sectores disponibles
     */
    public List<String> getAvailableSectors() {
        return serviceCache.keySet().stream().sorted().toList();
    }
    
    /**
     * Verifica si existe un servicio para el sector especificado.
     * 
     * @param sector El sector a verificar
     * @return true si existe un servicio especializado para el sector
     */
    public boolean hasSectorService(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            return false;
        }
        return serviceCache.containsKey(sector.toLowerCase().trim());
    }
    
    /**
     * Obtiene información sobre el servicio actualmente en uso para un sector.
     * 
     * @param sector El sector a consultar
     * @return Información del servicio
     */
    public ServiceInfo getServiceInfo(String sector) {
        AntiHallucinationService service = getService(sector);
        String actualSector = service.getSector();
        boolean isSpecialized = !actualSector.equals("default");
        
        return new ServiceInfo(
            actualSector,
            service.getClass().getSimpleName(),
            isSpecialized,
            isSpecialized ? "Servicio especializado" : "Servicio genérico por defecto"
        );
    }
    
    private void initializeServices() {
        log.info("Inicializando servicios anti-alucinación disponibles...");
        
        try {
            // Buscar todos los beans que implementen AntiHallucinationService
            Map<String, AntiHallucinationService> services = applicationContext.getBeansOfType(AntiHallucinationService.class);
            
            for (Map.Entry<String, AntiHallucinationService> entry : services.entrySet()) {
                String beanName = entry.getKey();
                AntiHallucinationService service = entry.getValue();
                
                // Omitir el factory mismo y el servicio por defecto
                if (service instanceof AntiHallucinationFactory || 
                    service instanceof DefaultAntiHallucinationService) {
                    continue;
                }
                
                String sector = service.getSector();
                if (sector != null && !sector.trim().isEmpty()) {
                    serviceCache.put(sector.toLowerCase(), service);
                    log.info("Registrado servicio '{}' para sector: {}", beanName, sector);
                }
            }
            
            log.info("Inicialización completada. Sectores disponibles: {}", getAvailableSectors());
            
        } catch (Exception e) {
            log.error("Error inicializando servicios anti-alucinación", e);
        }
    }
    
    /**
     * Clase para información del servicio.
     */
    public static class ServiceInfo {
        private final String sector;
        private final String className;
        private final boolean specialized;
        private final String description;
        
        public ServiceInfo(String sector, String className, boolean specialized, String description) {
            this.sector = sector;
            this.className = className;
            this.specialized = specialized;
            this.description = description;
        }
        
        public String getSector() { return sector; }
        public String getClassName() { return className; }
        public boolean isSpecialized() { return specialized; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("ServiceInfo{sector='%s', className='%s', specialized=%s, description='%s'}", 
                               sector, className, specialized, description);
        }
    }
    
    /**
     * Servicio por defecto cuando no hay uno especializado para el sector.
     */
    private static class DefaultAntiHallucinationService implements AntiHallucinationService {
        
        private static final Logger log = LoggerFactory.getLogger(DefaultAntiHallucinationService.class);
        
        private static final String DEFAULT_PROMPT_TEMPLATE = """
            INSTRUCCIONES: Analiza la información proporcionada y responde directamente la pregunta.
            
            INFORMACIÓN DE DOCUMENTOS:
            {context}
            
            PREGUNTA: {question}
            
            ANÁLISIS Y RESPUESTA DIRECTA:
            """;
        
        @Override
        public String createPrompt(String question, String context) {
            log.debug("Creando prompt genérico para pregunta: {}", question);
            
            if (context == null || context.trim().isEmpty()) {
                context = "No hay contexto disponible.";
            }
            
            return DEFAULT_PROMPT_TEMPLATE
                    .replace("{context}", context.trim())
                    .replace("{question}", question.trim());
        }
        
        @Override
        public boolean validateResponse(String response) {
            if (response == null || response.trim().isEmpty()) {
                return false;
            }
            
            // Validación básica
            boolean appropriateLength = response.length() >= 20 && response.length() <= 3000;
            boolean notOnlyNoInfo = !(response.toLowerCase().contains("no encontré") && response.length() < 50);
            
            return appropriateLength && notOnlyNoInfo;
        }
        
        @Override
        public String createFallbackResponse(String question) {
            return String.format(
                "No encontré información específica sobre \"%s\" en los documentos disponibles. " +
                "Para obtener una respuesta más precisa, asegúrate de que los documentos " +
                "relevantes estén cargados en el sistema.",
                question
            );
        }
        
        @Override
        public String getSector() {
            return "default";
        }
    }
}