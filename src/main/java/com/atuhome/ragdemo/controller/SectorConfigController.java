package com.atuhome.ragdemo.controller;

import com.atuhome.ragdemo.model.dto.request.SectorConfigRequest;
import com.atuhome.ragdemo.model.dto.response.SectorConfigResponse;
import com.atuhome.ragdemo.service.ai.AntiHallucinationFactory;
import com.atuhome.ragdemo.service.config.SectorConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestionar la configuración de sectores en el sistema RAG.
 * Permite cambiar dinámicamente entre diferentes servicios anti-alucinación especializados.
 */
@RestController
@RequestMapping("/api/config/sector")
@Tag(name = "Configuración de Sectores", description = "Gestión de configuraciones específicas por sector")
public class SectorConfigController {
    
    private static final Logger log = LoggerFactory.getLogger(SectorConfigController.class);
    
    private final SectorConfigurationService configurationService;
    private final AntiHallucinationFactory antiHallucinationFactory;
    
    public SectorConfigController(SectorConfigurationService configurationService,
                                AntiHallucinationFactory antiHallucinationFactory) {
        this.configurationService = configurationService;
        this.antiHallucinationFactory = antiHallucinationFactory;
    }
    
    @Operation(
        summary = "Obtener sectores disponibles",
        description = "Lista todos los sectores disponibles en el sistema con sus servicios especializados"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de sectores obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class)))
    })
    @GetMapping("/available")
    public ResponseEntity<SectorConfigResponse> getAvailableSectors() {
        log.info("Solicitando lista de sectores disponibles");
        
        List<String> availableSectors = configurationService.getAvailableSectors();
        String defaultSector = configurationService.getDefaultSector();
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(defaultSector)
                .availableSectors(availableSectors)
                .serviceInfo(antiHallucinationFactory.getServiceInfo(defaultSector))
                .message("Sectores disponibles obtenidos exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Obtener configuración actual",
        description = "Obtiene la configuración de sector actualmente aplicada para la sesión o sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class)))
    })
    @GetMapping("/current")
    public ResponseEntity<SectorConfigResponse> getCurrentConfiguration(
            @Parameter(description = "ID de sesión") @RequestParam(required = false) String sessionId,
            @Parameter(description = "ID de organización") @RequestParam(required = false) String organizationId,
            HttpServletRequest request) {
        
        // Si no se proporciona sessionId, usar el ID de sesión HTTP
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = request.getSession().getId();
        }
        
        log.info("Obteniendo configuración actual para sesión: {}, organización: {}", sessionId, organizationId);
        
        String effectiveSector = configurationService.getEffectiveSector(sessionId, organizationId);
        SectorConfigurationService.SectorConfiguration sessionConfig = 
            configurationService.getSessionConfiguration(sessionId);
        
        // Determinar fuente de configuración
        String configSource = "default";
        SectorConfigurationService.SectorSettings settings = new SectorConfigurationService.SectorSettings();
        
        if (sessionConfig != null) {
            configSource = "session";
            settings = sessionConfig.getSettings();
        }
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(effectiveSector)
                .serviceInfo(antiHallucinationFactory.getServiceInfo(effectiveSector))
                .configuration(
                    settings.getSimilarityThreshold(),
                    settings.getMaxResults(),
                    settings.isStrictValidation(),
                    settings.getCustomPromptId(),
                    configSource
                )
                .availableSectors(configurationService.getAvailableSectors())
                .message("Configuración actual obtenida exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Configurar sector por defecto del sistema",
        description = "Establece el sector por defecto que será usado cuando no hay configuración específica"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sector por defecto configurado exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class))),
        @ApiResponse(responseCode = "400", description = "Sector no válido o parámetros incorrectos")
    })
    @PostMapping("/default")
    public ResponseEntity<SectorConfigResponse> setDefaultSector(
            @Valid @RequestBody SectorConfigRequest request) {
        
        log.info("Configurando sector por defecto: {}", request.getSector());
        
        boolean success = configurationService.setDefaultSector(request.getSector());
        
        if (!success) {
            return ResponseEntity.badRequest()
                .body(new SectorConfigResponse(null, "Sector no válido: " + request.getSector()));
        }
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(request.getSector())
                .serviceInfo(antiHallucinationFactory.getServiceInfo(request.getSector()))
                .availableSectors(configurationService.getAvailableSectors())
                .message("Sector por defecto configurado exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Configurar sector para sesión",
        description = "Establece la configuración de sector específica para una sesión de usuario"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración de sesión establecida exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parámetros de configuración no válidos")
    })
    @PostMapping("/session")
    public ResponseEntity<SectorConfigResponse> setSessionConfiguration(
            @Valid @RequestBody SectorConfigRequest request,
            HttpServletRequest httpRequest) {
        
        // Si no se proporciona sessionId en el request, usar el de la sesión HTTP
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = httpRequest.getSession().getId();
        }
        
        log.info("Configurando sector para sesión {}: {}", sessionId, request.getSector());
        
        // Crear configuración personalizada
        SectorConfigurationService.SectorSettings settings = new SectorConfigurationService.SectorSettings();
        
        if (request.getSimilarityThreshold() != null) {
            settings.setSimilarityThreshold(request.getSimilarityThreshold());
        }
        if (request.getMaxResults() != null) {
            settings.setMaxResults(request.getMaxResults());
        }
        if (request.getStrictValidation() != null) {
            settings.setStrictValidation(request.getStrictValidation());
        }
        if (request.getCustomPromptId() != null) {
            settings.setCustomPromptId(request.getCustomPromptId());
        }
        
        boolean success = configurationService.setSessionSector(sessionId, request.getSector(), settings);
        
        if (!success) {
            return ResponseEntity.badRequest()
                .body(new SectorConfigResponse(null, "Error configurando sesión: parámetros no válidos"));
        }
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(request.getSector())
                .serviceInfo(antiHallucinationFactory.getServiceInfo(request.getSector()))
                .configuration(
                    settings.getSimilarityThreshold(),
                    settings.getMaxResults(),
                    settings.isStrictValidation(),
                    settings.getCustomPromptId(),
                    "session"
                )
                .availableSectors(configurationService.getAvailableSectors())
                .message("Configuración de sesión establecida exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Configurar sector para organización",
        description = "Establece la configuración de sector para toda una organización"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración de organización establecida exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parámetros de configuración no válidos")
    })
    @PostMapping("/organization")
    public ResponseEntity<SectorConfigResponse> setOrganizationConfiguration(
            @Valid @RequestBody SectorConfigRequest request) {
        
        if (request.getOrganizationId() == null || request.getOrganizationId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new SectorConfigResponse(null, "ID de organización es obligatorio"));
        }
        
        log.info("Configurando sector para organización {}: {}", 
                request.getOrganizationId(), request.getSector());
        
        // Crear configuración personalizada
        SectorConfigurationService.SectorSettings settings = new SectorConfigurationService.SectorSettings();
        
        if (request.getSimilarityThreshold() != null) {
            settings.setSimilarityThreshold(request.getSimilarityThreshold());
        }
        if (request.getMaxResults() != null) {
            settings.setMaxResults(request.getMaxResults());
        }
        if (request.getStrictValidation() != null) {
            settings.setStrictValidation(request.getStrictValidation());
        }
        if (request.getCustomPromptId() != null) {
            settings.setCustomPromptId(request.getCustomPromptId());
        }
        
        boolean success = configurationService.setOrganizationSector(
            request.getOrganizationId(), request.getSector(), settings);
        
        if (!success) {
            return ResponseEntity.badRequest()
                .body(new SectorConfigResponse(null, "Error configurando organización: parámetros no válidos"));
        }
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(request.getSector())
                .serviceInfo(antiHallucinationFactory.getServiceInfo(request.getSector()))
                .configuration(
                    settings.getSimilarityThreshold(),
                    settings.getMaxResults(),
                    settings.isStrictValidation(),
                    settings.getCustomPromptId(),
                    "organization"
                )
                .availableSectors(configurationService.getAvailableSectors())
                .message("Configuración de organización establecida exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Obtener información de servicio por sector",
        description = "Obtiene información detallada sobre el servicio anti-alucinación de un sector específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Información de servicio obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = SectorConfigResponse.class)))
    })
    @GetMapping("/service-info/{sector}")
    public ResponseEntity<SectorConfigResponse> getSectorServiceInfo(
            @Parameter(description = "Sector a consultar", example = "legal") 
            @PathVariable String sector) {
        
        log.info("Obteniendo información de servicio para sector: {}", sector);
        
        AntiHallucinationFactory.ServiceInfo serviceInfo = 
            antiHallucinationFactory.getServiceInfo(sector);
        
        SectorConfigResponse response = SectorConfigResponse.builder()
                .currentSector(sector)
                .serviceInfo(serviceInfo)
                .availableSectors(configurationService.getAvailableSectors())
                .message("Información de servicio obtenida exitosamente")
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Limpiar configuraciones de sesión expiradas",
        description = "Elimina configuraciones de sesión que han expirado (útil para mantenimiento)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Limpieza completada exitosamente")
    })
    @PostMapping("/cleanup")
    public ResponseEntity<SectorConfigResponse> cleanupExpiredSessions(
            @Parameter(description = "Edad máxima en horas", example = "24") 
            @RequestParam(defaultValue = "24") int maxAgeHours) {
        
        log.info("Iniciando limpieza de configuraciones expiradas (max age: {} horas)", maxAgeHours);
        
        long maxAgeMs = maxAgeHours * 60 * 60 * 1000L;
        int cleaned = configurationService.cleanupExpiredSessions(maxAgeMs);
        
        SectorConfigResponse response = new SectorConfigResponse(
            configurationService.getDefaultSector(),
            String.format("Limpieza completada: %d configuraciones eliminadas", cleaned)
        );
        
        return ResponseEntity.ok(response);
    }
}