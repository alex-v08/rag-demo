package com.atuhome.ragdemo.service.ai;

import com.atuhome.ragdemo.model.dto.response.ModelInfoResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModelManagementService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    @Lazy
    private DynamicChatService dynamicChatService;
    
    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;
    
    @Value("${spring.ai.ollama.chat.options.model}")
    private String defaultChatModel;
    
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String currentEmbeddingModel;
    
    private String currentChatModel;
    
    @PostConstruct
    public void init() {
        this.currentChatModel = defaultChatModel;
    }
    
    public List<ModelInfoResponse> getAvailableModels() {
        try {
            log.info("Obteniendo lista de modelos disponibles de Ollama");
            
            String url = ollamaBaseUrl + "/api/tags";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            List<ModelInfoResponse> models = new ArrayList<>();
            
            if (response != null && response.containsKey("models")) {
                List<Map<String, Object>> modelList = (List<Map<String, Object>>) response.get("models");
                
                for (Map<String, Object> model : modelList) {
                    String name = (String) model.get("name");
                    String digest = (String) model.get("digest");
                    Long size = ((Number) model.get("size")).longValue();
                    String modified = (String) model.get("modified_at");
                    
                    ModelInfoResponse modelInfo = ModelInfoResponse.builder()
                            .name(name)
                            .id(digest != null ? digest.substring(0, Math.min(12, digest.length())) : "")
                            .size(formatSize(size))
                            .modified(formatDate(modified))
                            .active(name.equals(currentChatModel))
                            .build();
                    
                    models.add(modelInfo);
                }
            }
            
            log.info("Se encontraron {} modelos disponibles", models.size());
            return models;
            
        } catch (Exception e) {
            log.error("Error obteniendo modelos disponibles: {}", e.getMessage());
            throw new RuntimeException("Error al obtener modelos disponibles", e);
        }
    }
    
    public ModelInfoResponse getCurrentModel() {
        return ModelInfoResponse.builder()
                .name(currentChatModel)
                .active(true)
                .build();
    }
    
    public ModelInfoResponse changeModel(String modelName) {
        try {
            log.info("Cambiando modelo de {} a {}", currentChatModel, modelName);
            
            // Verificar que el modelo existe
            List<ModelInfoResponse> availableModels = getAvailableModels();
            ModelInfoResponse targetModel = availableModels.stream()
                    .filter(m -> m.getName().equals(modelName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Modelo no encontrado: " + modelName));
            
            // Actualizar el modelo actual en memoria
            this.currentChatModel = modelName;
            targetModel.setActive(true);
            
            // Actualizar el chat client dinámicamente
            if (dynamicChatService != null) {
                dynamicChatService.updateChatClient();
            }
            
            log.info("Modelo cambiado exitosamente a {} (Nota: Este cambio es solo para esta sesión. Para hacer el cambio permanente, actualice application.properties)", modelName);
            return targetModel;
            
        } catch (Exception e) {
            log.error("Error cambiando modelo: {}", e.getMessage());
            throw new RuntimeException("Error al cambiar modelo", e);
        }
    }
    
    private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1) {
            return String.format("%.1f GB", gb);
        }
        
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.1f MB", mb);
    }
    
    private String formatDate(String isoDate) {
        // Simplificación - en producción usar DateTimeFormatter
        if (isoDate == null) return "Unknown";
        return "Recently modified";
    }
    
    public List<String> getAvailableChatModelNames() {
        try {
            List<ModelInfoResponse> models = getAvailableModels();
            
            // Filtrar solo modelos de chat (excluir embeddings)
            return models.stream()
                    .map(ModelInfoResponse::getName)
                    .filter(name -> !name.contains("embed") && !name.contains("bge"))
                    .toList();
                    
        } catch (Exception e) {
            log.error("Error obteniendo nombres de modelos: {}", e.getMessage());
            return List.of(currentChatModel); // Retornar al menos el modelo actual
        }
    }
    
    public String getCurrentChatModel() {
        return currentChatModel;
    }
}