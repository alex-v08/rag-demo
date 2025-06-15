package com.atuhome.ragdemo.service.ai;

/**
 * Interfaz para servicios de anti-alucinación especializados por sector.
 * Cada implementación proporciona prompts y validaciones específicas para un dominio.
 */
public interface AntiHallucinationService {
    
    /**
     * Crea un prompt específico del sector para generar respuestas contextualizadas.
     * 
     * @param question La pregunta del usuario
     * @param context El contexto extraído de los documentos
     * @return El prompt optimizado para el sector específico
     */
    String createPrompt(String question, String context);
    
    /**
     * Valida si la respuesta cumple con los criterios de calidad del sector.
     * 
     * @param response La respuesta generada por el LLM
     * @return true si la respuesta es válida, false en caso contrario
     */
    boolean validateResponse(String response);
    
    /**
     * Crea una respuesta de fallback cuando la validación falla.
     * 
     * @param question La pregunta original del usuario
     * @return Una respuesta de fallback apropiada para el sector
     */
    String createFallbackResponse(String question);
    
    /**
     * Obtiene el sector al que pertenece este servicio.
     * 
     * @return El nombre del sector (ej: "legal", "medical", "education")
     */
    String getSector();
    
    /**
     * Calcula una puntuación de confianza para la respuesta.
     * 
     * @param response La respuesta a evaluar
     * @param context El contexto utilizado
     * @return Puntuación entre 0.0 y 1.0
     */
    default double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        // Implementación básica - las clases especializadas pueden sobrescribir
        double score = 0.5;
        
        // Verificar longitud apropiada
        if (response.length() > 50 && response.length() < 2000) {
            score += 0.2;
        }
        
        // Verificar que no sea solo "no encontré información"
        if (!response.toLowerCase().contains("no encontré") && 
            !response.toLowerCase().contains("no hay información")) {
            score += 0.3;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
}