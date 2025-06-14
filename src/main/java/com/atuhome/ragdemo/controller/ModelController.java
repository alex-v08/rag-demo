package com.atuhome.ragdemo.controller;

import com.atuhome.ragdemo.model.dto.request.ModelChangeRequest;
import com.atuhome.ragdemo.model.dto.response.ModelInfoResponse;
import com.atuhome.ragdemo.service.ai.ModelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "Model Management", description = "Endpoints para gestión de modelos LLM")
public class ModelController {
    
    private final ModelManagementService modelManagementService;
    
    @GetMapping("/available")
    @Operation(summary = "Listar modelos disponibles", 
               description = "Obtiene la lista de modelos disponibles en Ollama")
    public ResponseEntity<List<ModelInfoResponse>> getAvailableModels() {
        return ResponseEntity.ok(modelManagementService.getAvailableModels());
    }
    
    @GetMapping("/available/chat")
    @Operation(summary = "Listar modelos de chat disponibles", 
               description = "Obtiene solo los modelos de chat (no embeddings) disponibles")
    public ResponseEntity<List<String>> getAvailableChatModels() {
        return ResponseEntity.ok(modelManagementService.getAvailableChatModelNames());
    }
    
    @GetMapping("/current")
    @Operation(summary = "Obtener modelo actual", 
               description = "Obtiene información del modelo actualmente configurado")
    public ResponseEntity<ModelInfoResponse> getCurrentModel() {
        return ResponseEntity.ok(modelManagementService.getCurrentModel());
    }
    
    @PostMapping("/change")
    @Operation(summary = "Cambiar modelo", 
               description = "Cambia el modelo LLM a utilizar")
    public ResponseEntity<ModelInfoResponse> changeModel(@RequestBody ModelChangeRequest request) {
        return ResponseEntity.ok(modelManagementService.changeModel(request.getModelName()));
    }
}