package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AntiHallucinationService {

    private static final Logger log = LoggerFactory.getLogger(AntiHallucinationService.class);

    private static final String STRICT_PROMPT_TEMPLATE = """
        Eres un asistente que ayuda a responder preguntas basándose en documentos proporcionados.
        
        CONTEXTO DE LOS DOCUMENTOS:
        {context}
        
        PREGUNTA DEL USUARIO:
        {question}
        
        INSTRUCCIONES:
        - Responde la pregunta basándote en la información del contexto anterior
        - Si la información está en el contexto, úsala para dar una respuesta completa
        - Cita las fuentes usando [Documento: nombre_archivo]
        - Si realmente no hay información relevante en el contexto, entonces di que no la encontraste
        
        RESPUESTA:
        """;

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[Documento:.*?\\]");
    private static final Pattern UNCERTAINTY_PATTERN = Pattern.compile(
        "(?i)(no encontré|no hay información|no está disponible|no puedo|insuficiente)"
    );

    public String createStrictPrompt(String question, String context) {
        log.debug("Creando prompt estricto para pregunta: {}", question);
        
        if (context == null || context.trim().isEmpty()) {
            log.warn("Contexto vacío para la pregunta: {}", question);
            context = "No hay contexto disponible.";
        }
        
        return STRICT_PROMPT_TEMPLATE
                .replace("{context}", context.trim())
                .replace("{question}", question.trim());
    }

    public boolean validateResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Respuesta vacía detectada");
            return false;
        }
        
        log.debug("Validando respuesta: {}", response.substring(0, Math.min(100, response.length())));
        
        // Verificar que contiene citas o admite incertidumbre
        boolean hasCitations = CITATION_PATTERN.matcher(response).find();
        boolean admitsUncertainty = UNCERTAINTY_PATTERN.matcher(response).find();
        
        if (!hasCitations && !admitsUncertainty) {
            log.warn("Respuesta sin citas ni admisión de incertidumbre detectada");
            return false;
        }
        
        // Verificar que no contiene frases que indican invención
        if (containsInventiveLanguage(response)) {
            log.warn("Respuesta con lenguaje inventivo detectada");
            return false;
        }
        
        // Verificar longitud razonable
        if (response.length() > 2000) {
            log.warn("Respuesta demasiado larga detectada: {} caracteres", response.length());
            return false;
        }
        
        log.debug("Respuesta validada exitosamente");
        return true;
    }

    private boolean containsInventiveLanguage(String response) {
        String[] inventivePhrases = {
            "generalmente", "usualmente", "típicamente", "normalmente",
            "según mi conocimiento", "en mi experiencia", "como se sabe",
            "es común que", "suele ser", "por lo general",
            "tradicionalmente", "históricamente", "comúnmente"
        };
        
        String lowerResponse = response.toLowerCase();
        for (String phrase : inventivePhrases) {
            if (lowerResponse.contains(phrase)) {
                log.debug("Frase inventiva detectada: '{}'", phrase);
                return true;
            }
        }
        
        return false;
    }

    public String createFallbackResponse(String question) {
        return String.format(
            "No encontré información específica sobre \"%s\" en los documentos disponibles. " +
            "Para obtener una respuesta precisa, asegúrate de que los documentos relevantes " +
            "estén cargados en el sistema.",
            question
        );
    }

    public String enhancePromptWithExamples(String basePrompt) {
        String examples = """
            
            EJEMPLOS DE RESPUESTAS CORRECTAS:
            
            Pregunta: "¿Cuál es el plazo para presentar la demanda?"
            Respuesta correcta: "Según el artículo 15 del documento, el plazo para presentar la demanda es de 30 días hábiles. [Documento: codigo_procesal.pdf]"
            
            Pregunta: "¿Qué dice sobre contratos internacionales?"
            Respuesta correcta: "No encontré información sobre contratos internacionales en los documentos disponibles."
            
            EJEMPLOS DE RESPUESTAS INCORRECTAS (NO HACER):
            - "Generalmente, los plazos suelen ser de 30 días..." (SIN CITA)
            - "Según mi conocimiento legal..." (CONOCIMIENTO EXTERNO)
            - "Aunque no está en el documento, normalmente..." (INVENCIÓN)
            
            """;
        
        return basePrompt + examples;
    }

    public double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        double score = 0.5; // Puntuación base
        
        // Puntos por tener citas
        if (CITATION_PATTERN.matcher(response).find()) {
            score += 0.3;
        }
        
        // Puntos por admitir incertidumbre cuando es apropiado
        if (UNCERTAINTY_PATTERN.matcher(response).find()) {
            score += 0.1;
        }
        
        // Penalización por lenguaje inventivo
        if (containsInventiveLanguage(response)) {
            score -= 0.4;
        }
        
        // Verificar que el contenido de la respuesta está relacionado con el contexto
        String[] responseWords = response.toLowerCase().split("\\W+");
        String[] contextWords = context.toLowerCase().split("\\W+");
        
        long matchingWords = 0;
        for (String responseWord : responseWords) {
            if (responseWord.length() > 3) { // Ignorar palabras muy cortas
                for (String contextWord : contextWords) {
                    if (contextWord.equals(responseWord)) {
                        matchingWords++;
                        break;
                    }
                }
            }
        }
        
        // Porcentaje de palabras coincidentes
        double wordMatchRatio = (double) matchingWords / Math.max(responseWords.length, 1);
        score += wordMatchRatio * 0.2;
        
        return Math.max(0.0, Math.min(1.0, score));
    }
}