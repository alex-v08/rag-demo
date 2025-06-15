package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio anti-alucinación especializado para el sector educativo.
 * Enfoca las respuestas en metodologías pedagógicas y contenido didáctico claro.
 */
@Component("educationAntiHallucination")
public class EducationAntiHallucinationService implements AntiHallucinationService {

    private static final Logger log = LoggerFactory.getLogger(EducationAntiHallucinationService.class);
    
    private static final String EDUCATION_PROMPT_TEMPLATE = """
        INSTRUCCIONES: Analiza la información educativa proporcionada y responde directamente la pregunta.
        
        Como educador especializado, tu objetivo es:
        - Proporcionar respuestas pedagógicamente estructuradas y didácticas
        - Usar un lenguaje claro y apropiado para el nivel educativo
        - Incluir ejemplos prácticos cuando sea relevante
        - Estructurar la información de manera progresiva y comprensible
        - Citar fuentes educativas y material pedagógico específico
        - Fomentar el pensamiento crítico y la comprensión profunda
        
        MATERIAL PEDAGÓGICO:
        {context}
        
        PREGUNTA EDUCATIVA: {question}
        
        RESPUESTA DIDÁCTICA Y ESTRUCTURADA:
        """;
    
    private static final Pattern EDUCATIONAL_STRUCTURE_PATTERN = Pattern.compile(
        "(?i)(objetivo|competencia|habilidad|conocimiento|aprendizaje|enseñanza|evaluación|metodología|didáctica)"
    );
    
    private static final Pattern PEDAGOGICAL_TERMS_PATTERN = Pattern.compile(
        "(?i)(explicar|demostrar|analizar|sintetizar|aplicar|comprender|recordar|evaluar|crear|ejemplo|ejercicio)"
    );
    
    private static final Pattern EDUCATIONAL_ORGANIZATION_PATTERN = Pattern.compile(
        "(?i)(primero|segundo|tercero|pasos|etapas|niveles|módulos|unidades|capítulos|secciones)"
    );

    @Override
    public String createPrompt(String question, String context) {
        log.debug("Creando prompt educativo para pregunta: {}", question);
        
        if (context == null || context.trim().isEmpty()) {
            log.warn("Contexto educativo vacío para la pregunta: {}", question);
            context = "No hay material pedagógico disponible.";
        }
        
        return EDUCATION_PROMPT_TEMPLATE
                .replace("{context}", context.trim())
                .replace("{question}", question.trim());
    }

    @Override
    public boolean validateResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Respuesta educativa vacía detectada");
            return false;
        }
        
        log.debug("Validando respuesta educativa: {}", response.substring(0, Math.min(100, response.length())));
        
        // Verificar que la respuesta no sea demasiado genérica
        if (response.toLowerCase().contains("no encontré información") && response.length() < 100) {
            return true; // Respuesta de no información es válida
        }
        
        // Para respuestas substantivas, verificar elementos educativos
        boolean hasEducationalStructure = EDUCATIONAL_STRUCTURE_PATTERN.matcher(response).find();
        boolean hasPedagogicalTerms = PEDAGOGICAL_TERMS_PATTERN.matcher(response).find();
        boolean hasOrganization = EDUCATIONAL_ORGANIZATION_PATTERN.matcher(response).find() ||
                                response.contains("[Documento:");
        
        // Verificar longitud apropiada para respuestas educativas
        boolean appropriateLength = response.length() >= 50 && response.length() <= 3500;
        
        // Verificar estructura didáctica (puntos, ejemplos, organización)
        boolean hasDidacticStructure = hasDidacticElements(response);
        
        // Evitar respuestas que contengan frases de invención
        boolean noInventiveLanguage = !containsInventiveLanguage(response);
        
        boolean isValid = (hasEducationalStructure || hasPedagogicalTerms || hasOrganization) && 
                         appropriateLength && 
                         noInventiveLanguage &&
                         hasDidacticStructure;
        
        if (!isValid) {
            log.warn("Respuesta educativa falló validación: hasStructure={}, hasPedagogical={}, hasOrganization={}, appropriateLength={}, hasDidactic={}, noInventive={}", 
                    hasEducationalStructure, hasPedagogicalTerms, hasOrganization, appropriateLength, hasDidacticStructure, noInventiveLanguage);
        }
        
        return isValid;
    }

    @Override
    public String createFallbackResponse(String question) {
        return String.format(
            "No encontré información específica sobre \"%s\" en el material pedagógico disponible. " +
            "Para obtener una respuesta educativa completa, sería recomendable consultar fuentes " +
            "académicas especializadas, libros de texto actualizados o contactar con un docente " +
            "especializado en la materia.",
            question
        );
    }

    @Override
    public String getSector() {
        return "education";
    }

    @Override
    public double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        double score = 0.3; // Base score
        
        // Puntos por terminología educativa
        if (EDUCATIONAL_STRUCTURE_PATTERN.matcher(response).find()) {
            score += 0.25;
        }
        
        // Puntos por términos pedagógicos
        if (PEDAGOGICAL_TERMS_PATTERN.matcher(response).find()) {
            score += 0.25;
        }
        
        // Puntos por organización educativa
        if (EDUCATIONAL_ORGANIZATION_PATTERN.matcher(response).find()) {
            score += 0.2;
        }
        
        // Puntos por citas de documentos
        if (response.contains("[Documento:")) {
            score += 0.15;
        }
        
        // Puntos por estructura didáctica
        if (hasDidacticElements(response)) {
            score += 0.15;
        }
        
        // Penalización por respuestas muy cortas sin contexto educativo
        if (response.length() < 100 && !response.toLowerCase().contains("no encontré") &&
            !EDUCATIONAL_STRUCTURE_PATTERN.matcher(response).find()) {
            score -= 0.3;
        }
        
        // Verificar correspondencia con contexto educativo
        String[] educationalTerms = {"educación", "enseñanza", "aprendizaje", "estudiante", "curso", "programa", "módulo"};
        int contextMatches = 0;
        String lowerContext = context.toLowerCase();
        String lowerResponse = response.toLowerCase();
        
        for (String term : educationalTerms) {
            if (lowerContext.contains(term) && lowerResponse.contains(term)) {
                contextMatches++;
            }
        }
        
        if (contextMatches > 0) {
            score += Math.min(0.25, contextMatches * 0.05);
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private boolean hasDidacticElements(String response) {
        // Verificar si la respuesta tiene elementos didácticos como:
        // - Listas numeradas o con viñetas
        // - Ejemplos
        // - Estructura clara
        
        boolean hasNumberedList = response.matches(".*\\d+\\s*[.)].+");
        boolean hasBulletPoints = response.contains("•") || response.contains("-") || response.contains("*");
        boolean hasExamples = response.toLowerCase().contains("ejemplo") || 
                             response.toLowerCase().contains("por ejemplo") ||
                             response.toLowerCase().contains("como:");
        boolean hasStructuredContent = response.contains(":") && response.contains("\n");
        
        return hasNumberedList || hasBulletPoints || hasExamples || hasStructuredContent;
    }
    
    private boolean containsInventiveLanguage(String response) {
        String[] inventivePhrases = {
            "generalmente", "usualmente", "típicamente", "normalmente",
            "según mi experiencia", "en mi experiencia docente", "como educador",
            "según mi conocimiento", "como se sabe", "es común que", "suele ser",
            "tradicionalmente", "históricamente", "comúnmente"
        };
        
        String lowerResponse = response.toLowerCase();
        for (String phrase : inventivePhrases) {
            if (lowerResponse.contains(phrase)) {
                log.debug("Frase inventiva detectada en respuesta educativa: '{}'", phrase);
                return true;
            }
        }
        
        return false;
    }
}