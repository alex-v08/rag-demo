package com.atuhome.ragdemo.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información sobre un modelo LLM")
public class ModelInfoResponse {
    
    @Schema(description = "Nombre del modelo", example = "llama3.2:latest")
    private String name;
    
    @Schema(description = "ID del modelo", example = "a80c4f17acd5")
    private String id;
    
    @Schema(description = "Tamaño del modelo", example = "2.0 GB")
    private String size;
    
    @Schema(description = "Fecha de modificación", example = "About a minute ago")
    private String modified;
    
    @Schema(description = "Indica si es el modelo actualmente activo")
    private boolean active;
}