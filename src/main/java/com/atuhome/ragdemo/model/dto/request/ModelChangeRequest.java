package com.atuhome.ragdemo.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request para cambiar el modelo LLM")
public class ModelChangeRequest {
    
    @NotBlank(message = "El nombre del modelo es requerido")
    @Schema(description = "Nombre del modelo a utilizar", example = "llama3.2:latest")
    private String modelName;
}