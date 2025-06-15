package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio anti-alucinación especializado para el sector de ventas comerciales.
 * Enfoca las respuestas en información comercial precisa, precios, productos y servicios.
 */
@Component("salesAntiHallucination")
public class SalesAntiHallucinationService implements AntiHallucinationService {

    private static final Logger log = LoggerFactory.getLogger(SalesAntiHallucinationService.class);
    
    private static final String SALES_PROMPT_TEMPLATE = """
        INSTRUCCIONES: Analiza la información comercial proporcionada y responde directamente la pregunta.
        
        Como asistente comercial especializado, tu objetivo es:
        - Proporcionar información precisa sobre productos, servicios y precios
        - Usar un lenguaje comercial profesional pero accesible
        - Destacar beneficios y características clave de productos/servicios
        - Incluir información sobre disponibilidad, términos y condiciones cuando sea relevante
        - Ser transparente sobre limitaciones o restricciones
        - Citar catálogos, listas de precios o documentación comercial específica
        - Mantener un tono profesional y orientado al cliente
        
        INFORMACIÓN COMERCIAL:
        {context}
        
        CONSULTA COMERCIAL: {question}
        
        RESPUESTA COMERCIAL PROFESIONAL:
        """;
    
    private static final Pattern COMMERCIAL_TERMS_PATTERN = Pattern.compile(
        "(?i)(producto|servicio|precio|costo|tarifa|oferta|promoción|descuento|garantía|disponibilidad|stock|entrega|envío)"
    );
    
    private static final Pattern SALES_LANGUAGE_PATTERN = Pattern.compile(
        "(?i)(beneficios|características|ventajas|calidad|solución|propuesta|recomendación|incluye|disponible)"
    );
    
    private static final Pattern PRICING_TERMS_PATTERN = Pattern.compile(
        "(?i)(\\$|€|€|precio|costo|tarifa|mensual|anual|por unidad|desde|hasta|a partir de|cotización)"
    );
    
    private static final Pattern AVAILABILITY_TERMS_PATTERN = Pattern.compile(
        "(?i)(disponible|stock|inventario|entrega|envío|tiempo|plazo|inmediato|bajo pedido)"
    );

    @Override
    public String createPrompt(String question, String context) {
        log.debug("Creando prompt comercial para pregunta: {}", question);
        
        if (context == null || context.trim().isEmpty()) {
            log.warn("Contexto comercial vacío para la pregunta: {}", question);
            context = "No hay información comercial disponible.";
        }
        
        return SALES_PROMPT_TEMPLATE
                .replace("{context}", context.trim())
                .replace("{question}", question.trim());
    }

    @Override
    public boolean validateResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Respuesta comercial vacía detectada");
            return false;
        }
        
        log.debug("Validando respuesta comercial: {}", response.substring(0, Math.min(100, response.length())));
        
        // Verificar que la respuesta no sea demasiado genérica
        if (response.toLowerCase().contains("no encontré información") && response.length() < 100) {
            return true; // Respuesta de no información es válida
        }
        
        // Para respuestas substantivas, verificar elementos comerciales
        boolean hasCommercialTerms = COMMERCIAL_TERMS_PATTERN.matcher(response).find();
        boolean hasSalesLanguage = SALES_LANGUAGE_PATTERN.matcher(response).find();
        boolean hasDocumentCitation = response.contains("[Documento:");
        
        // Verificar longitud apropiada para respuestas comerciales
        boolean appropriateLength = response.length() >= 50 && response.length() <= 3000;
        
        // Verificar que no haga promesas no fundamentadas
        boolean noUnfoundedPromises = !containsUnfoundedPromises(response);
        
        // Evitar respuestas que contengan frases de invención comercial
        boolean noInventiveLanguage = !containsInventiveLanguage(response);
        
        boolean isValid = (hasCommercialTerms || hasSalesLanguage || hasDocumentCitation) && 
                         appropriateLength && 
                         noUnfoundedPromises &&
                         noInventiveLanguage;
        
        if (!isValid) {
            log.warn("Respuesta comercial falló validación: hasCommercial={}, hasSales={}, hasDoc={}, appropriateLength={}, noPromises={}, noInventive={}", 
                    hasCommercialTerms, hasSalesLanguage, hasDocumentCitation, appropriateLength, noUnfoundedPromises, noInventiveLanguage);
        }
        
        return isValid;
    }

    @Override
    public String createFallbackResponse(String question) {
        return String.format(
            "No encontré información comercial específica sobre \"%s\" en nuestros catálogos y documentación disponible. " +
            "Para obtener información actualizada sobre productos, servicios y precios, te recomiendo contactar " +
            "directamente con nuestro equipo comercial que podrá proporcionarte detalles precisos y " +
            "ofertas personalizadas según tus necesidades.",
            question
        );
    }

    @Override
    public String getSector() {
        return "sales";
    }

    @Override
    public double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        double score = 0.3; // Base score
        
        // Puntos por terminología comercial
        if (COMMERCIAL_TERMS_PATTERN.matcher(response).find()) {
            score += 0.25;
        }
        
        // Puntos por lenguaje de ventas apropiado
        if (SALES_LANGUAGE_PATTERN.matcher(response).find()) {
            score += 0.2;
        }
        
        // Puntos por información de precios específica
        if (PRICING_TERMS_PATTERN.matcher(response).find()) {
            score += 0.15;
        }
        
        // Puntos por información de disponibilidad
        if (AVAILABILITY_TERMS_PATTERN.matcher(response).find()) {
            score += 0.1;
        }
        
        // Puntos por citas de documentos comerciales
        if (response.contains("[Documento:")) {
            score += 0.2;
        }
        
        // Puntos por estructura profesional
        if (hasProfessionalStructure(response)) {
            score += 0.1;
        }
        
        // Penalización por respuestas muy cortas sin contexto comercial
        if (response.length() < 100 && !response.toLowerCase().contains("no encontré") &&
            !COMMERCIAL_TERMS_PATTERN.matcher(response).find()) {
            score -= 0.3;
        }
        
        // Penalización por promesas no fundamentadas
        if (containsUnfoundedPromises(response)) {
            score -= 0.4;
        }
        
        // Verificar correspondencia con contexto comercial
        String[] commercialTerms = {"producto", "servicio", "venta", "comercial", "cliente", "precio", "oferta"};
        int contextMatches = 0;
        String lowerContext = context.toLowerCase();
        String lowerResponse = response.toLowerCase();
        
        for (String term : commercialTerms) {
            if (lowerContext.contains(term) && lowerResponse.contains(term)) {
                contextMatches++;
            }
        }
        
        if (contextMatches > 0) {
            score += Math.min(0.2, contextMatches * 0.04);
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private boolean hasProfessionalStructure(String response) {
        // Verificar si la respuesta tiene estructura profesional comercial
        boolean hasStructuredInfo = response.contains(":") || response.contains("•") || response.contains("-");
        boolean hasCourtesyLanguage = response.toLowerCase().contains("le") || 
                                    response.toLowerCase().contains("usted") ||
                                    response.toLowerCase().contains("nuestro");
        
        return hasStructuredInfo || hasCourtesyLanguage;
    }
    
    private boolean containsUnfoundedPromises(String response) {
        String[] unfoundedPromises = {
            "garantizamos", "aseguramos", "prometemos", "sin costo", "gratis siempre",
            "mejor precio del mercado", "único en el mercado", "incomparable",
            "sin limitaciones", "ilimitado", "para siempre", "de por vida"
        };
        
        String lowerResponse = response.toLowerCase();
        for (String promise : unfoundedPromises) {
            if (lowerResponse.contains(promise)) {
                log.warn("Promesa no fundamentada detectada en respuesta comercial: '{}'", promise);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean containsInventiveLanguage(String response) {
        String[] inventivePhrases = {
            "generalmente vendemos", "usualmente ofrecemos", "típicamente cuesta",
            "según mi experiencia comercial", "en mi experiencia de ventas",
            "como vendedor", "según mi conocimiento", "como se sabe",
            "es común que cueste", "suele venderse por", "por lo general vale"
        };
        
        String lowerResponse = response.toLowerCase();
        for (String phrase : inventivePhrases) {
            if (lowerResponse.contains(phrase)) {
                log.debug("Frase inventiva detectada en respuesta comercial: '{}'", phrase);
                return true;
            }
        }
        
        return false;
    }
}