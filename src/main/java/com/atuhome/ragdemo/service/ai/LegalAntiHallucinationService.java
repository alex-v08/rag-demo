package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio anti-alucinación especializado para el sector legal.
 * Enfoca las respuestas en fundamentación jurídica y citas legales precisas.
 */
@Component("legalAntiHallucination")
public class LegalAntiHallucinationService implements AntiHallucinationService {

    private static final Logger log = LoggerFactory.getLogger(LegalAntiHallucinationService.class);
    
    private static final String LEGAL_PROMPT_TEMPLATE = """
        INSTRUCCIONES: Analiza la información legal proporcionada y responde directamente la pregunta.
        
        Como asistente legal especializado, tu objetivo es:
        - Proporcionar respuestas fundamentadas en la documentación legal proporcionada
        - Citar específicamente artículos, leyes o secciones relevantes
        - Usar terminología jurídica precisa
        - Indicar claramente cuando la información no está disponible
        
        DOCUMENTACIÓN LEGAL:
        {context}
        
        CONSULTA LEGAL: {question}
        
        RESPUESTA LEGAL FUNDAMENTADA:
        """;
    
    private static final Pattern LEGAL_CITATION_PATTERN = Pattern.compile(
        "(?i)(artículo|art\\.|ley|decreto|código|reglamento|norma|inciso|párrafo)"
    );
    
    private static final Pattern LEGAL_TERMINOLOGY_PATTERN = Pattern.compile(
        "(?i)(según|conforme|establece|dispone|prescribe|determina|regula)"
    );

    @Override
    public String createPrompt(String question, String context) {
        log.debug("Creando prompt legal para pregunta: {}", question);
        
        if (context == null || context.trim().isEmpty()) {
            log.warn("Contexto legal vacío para la pregunta: {}", question);
            context = "No hay documentación legal disponible.";
        }
        
        return LEGAL_PROMPT_TEMPLATE
                .replace("{context}", context.trim())
                .replace("{question}", question.trim());
    }

    @Override
    public boolean validateResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Respuesta legal vacía detectada");
            return false;
        }
        
        log.debug("Validando respuesta legal: {}", response.substring(0, Math.min(100, response.length())));
        
        // Verificar que la respuesta no sea demasiado genérica
        if (response.toLowerCase().contains("no encontré información") && response.length() < 100) {
            return true; // Respuesta de no información es válida
        }
        
        // Para respuestas substantivas, verificar elementos legales
        boolean hasLegalElements = LEGAL_CITATION_PATTERN.matcher(response).find() ||
                                 LEGAL_TERMINOLOGY_PATTERN.matcher(response).find() ||
                                 response.contains("[Documento:");
        
        // Verificar longitud apropiada para respuestas legales
        boolean appropriateLength = response.length() >= 50 && response.length() <= 3000;
        
        // Evitar respuestas que contengan frases de invención
        boolean noInventiveLanguage = !containsInventiveLanguage(response);
        
        boolean isValid = hasLegalElements && appropriateLength && noInventiveLanguage;
        
        if (!isValid) {
            log.warn("Respuesta legal falló validación: hasLegalElements={}, appropriateLength={}, noInventiveLanguage={}", 
                    hasLegalElements, appropriateLength, noInventiveLanguage);
        }
        
        return isValid;
    }

    @Override
    public String createFallbackResponse(String question) {
        return String.format(
            "No encontré información legal específica sobre \"%s\" en la documentación proporcionada. " +
            "Para obtener una respuesta jurídica precisa, es necesario consultar la normativa específica " +
            "aplicable o contactar con un asesor legal especializado.",
            question
        );
    }

    @Override
    public String getSector() {
        return "legal";
    }

    @Override
    public double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        double score = 0.3; // Base score
        
        // Puntos por citas legales
        if (LEGAL_CITATION_PATTERN.matcher(response).find()) {
            score += 0.3;
        }
        
        // Puntos por terminología legal apropiada
        if (LEGAL_TERMINOLOGY_PATTERN.matcher(response).find()) {
            score += 0.2;
        }
        
        // Puntos por citas de documentos
        if (response.contains("[Documento:")) {
            score += 0.2;
        }
        
        // Penalización por respuestas muy cortas (excepto "no encontré")
        if (response.length() < 100 && !response.toLowerCase().contains("no encontré")) {
            score -= 0.3;
        }
        
        // Verificar correspondencia con contexto legal
        String[] legalTerms = {"legal", "jurídico", "derecho", "ley", "artículo", "código"};
        int contextMatches = 0;
        String lowerContext = context.toLowerCase();
        String lowerResponse = response.toLowerCase();
        
        for (String term : legalTerms) {
            if (lowerContext.contains(term) && lowerResponse.contains(term)) {
                contextMatches++;
            }
        }
        
        if (contextMatches > 0) {
            score += Math.min(0.2, contextMatches * 0.05);
        }
        
        return Math.max(0.0, Math.min(1.0, score));
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
                log.debug("Frase inventiva detectada en respuesta legal: '{}'", phrase);
                return true;
            }
        }
        
        return false;
    }
}