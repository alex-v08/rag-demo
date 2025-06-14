package com.atuhome.ragdemo.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionRequest {
    
    @NotBlank(message = "La pregunta no puede estar vacía")
    @Size(min = 5, max = 1000, message = "La pregunta debe tener entre 5 y 1000 caracteres")
    private String question;
}