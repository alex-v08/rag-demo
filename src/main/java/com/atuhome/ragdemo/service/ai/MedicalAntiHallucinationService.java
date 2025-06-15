package com.atuhome.ragdemo.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Servicio anti-alucinación especializado para el sector médico.
 * Enfoca las respuestas en evidencia científica y terminología médica precisa.
 */
@Component("medicalAntiHallucination")
public class MedicalAntiHallucinationService implements AntiHallucinationService {

    private static final Logger log = LoggerFactory.getLogger(MedicalAntiHallucinationService.class);
    
    private static final String MEDICAL_PROMPT_TEMPLATE = """
        INSTRUCCIONES: Analiza la información médica proporcionada y responde directamente la pregunta.
        
        Como asistente médico especializado, tu objetivo es:
        - Proporcionar respuestas basadas exclusivamente en evidencia científica documentada
        - Usar terminología médica precisa y apropiada
        - Citar estudios, guías clínicas o protocolos específicos cuando sea relevante
        - Incluir advertencias sobre la necesidad de consulta médica profesional cuando sea apropiado
        - Ser claro sobre las limitaciones de la información disponible
        
        LITERATURA MÉDICA Y CIENTÍFICA:
        {context}
        
        CONSULTA MÉDICA: {question}
        
        RESPUESTA MÉDICA BASADA EN EVIDENCIA:
        """;
    
    private static final Pattern MEDICAL_TERMINOLOGY_PATTERN = Pattern.compile(
        "(?i)(diagnóstico|tratamiento|síntomas|patología|etiología|fisiopatología|terapia|medicamento|fármaco|dosis|contraindicación|efectos secundarios|pronóstico)"
    );
    
    private static final Pattern MEDICAL_EVIDENCE_PATTERN = Pattern.compile(
        "(?i)(estudio|investigación|ensayo clínico|metaanálisis|revisión sistemática|guía clínica|protocolo|evidencia|según|demuestra|indica)"
    );
    
    private static final Pattern SAFETY_DISCLAIMER_PATTERN = Pattern.compile(
        "(?i)(consulte.*médico|consulta.*profesional|atención médica|diagnóstico médico|no sustituye|recomendación médica)"
    );

    @Override
    public String createPrompt(String question, String context) {
        log.debug("Creando prompt médico para pregunta: {}", question);
        
        if (context == null || context.trim().isEmpty()) {
            log.warn("Contexto médico vacío para la pregunta: {}", question);
            context = "No hay literatura médica disponible.";
        }
        
        return MEDICAL_PROMPT_TEMPLATE
                .replace("{context}", context.trim())
                .replace("{question}", question.trim());
    }

    @Override
    public boolean validateResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("Respuesta médica vacía detectada");
            return false;
        }
        
        log.debug("Validando respuesta médica: {}", response.substring(0, Math.min(100, response.length())));
        
        // Verificar que la respuesta no sea demasiado genérica
        if (response.toLowerCase().contains("no encontré información") && response.length() < 100) {
            return true; // Respuesta de no información es válida
        }
        
        // Para respuestas substantivas, verificar elementos médicos
        boolean hasMedicalTerminology = MEDICAL_TERMINOLOGY_PATTERN.matcher(response).find();
        boolean hasEvidenceBasis = MEDICAL_EVIDENCE_PATTERN.matcher(response).find() ||
                                 response.contains("[Documento:");
        
        // Verificar longitud apropiada para respuestas médicas
        boolean appropriateLength = response.length() >= 50 && response.length() <= 4000;
        
        // Evitar respuestas que contengan frases de invención médica peligrosa
        boolean noInventiveLanguage = !containsInventiveMedicalLanguage(response);
        
        // Para temas médicos sensibles, verificar disclaimer de seguridad
        boolean needsSafetyDisclaimer = needsSafetyDisclaimer(response);
        boolean hasSafetyDisclaimer = SAFETY_DISCLAIMER_PATTERN.matcher(response).find();
        
        boolean safetyCheck = !needsSafetyDisclaimer || hasSafetyDisclaimer;
        
        boolean isValid = (hasMedicalTerminology || hasEvidenceBasis) && 
                         appropriateLength && 
                         noInventiveLanguage && 
                         safetyCheck;
        
        if (!isValid) {
            log.warn("Respuesta médica falló validación: hasTerminology={}, hasEvidence={}, appropriateLength={}, noInventive={}, safetyCheck={}", 
                    hasMedicalTerminology, hasEvidenceBasis, appropriateLength, noInventiveLanguage, safetyCheck);
        }
        
        return isValid;
    }

    @Override
    public String createFallbackResponse(String question) {
        return String.format(
            "No encontré información médica específica sobre \"%s\" en la literatura disponible. " +
            "Para obtener información médica precisa y actualizada, es fundamental consultar con un " +
            "profesional de la salud cualificado que pueda evaluar su situación particular. " +
            "Esta respuesta no constituye consejo médico profesional.",
            question
        );
    }

    @Override
    public String getSector() {
        return "medical";
    }

    @Override
    public double calculateConfidenceScore(String response, String context) {
        if (response == null || context == null) {
            return 0.0;
        }
        
        double score = 0.2; // Base score más conservador para medicina
        
        // Puntos por terminología médica
        if (MEDICAL_TERMINOLOGY_PATTERN.matcher(response).find()) {
            score += 0.3;
        }
        
        // Puntos por base de evidencia
        if (MEDICAL_EVIDENCE_PATTERN.matcher(response).find()) {
            score += 0.3;
        }
        
        // Puntos por citas de documentos
        if (response.contains("[Documento:")) {
            score += 0.2;
        }
        
        // Puntos por disclaimer de seguridad cuando es apropiado
        if (needsSafetyDisclaimer(response) && SAFETY_DISCLAIMER_PATTERN.matcher(response).find()) {
            score += 0.2;
        }
        
        // Penalización por respuestas muy cortas sin contexto médico
        if (response.length() < 100 && !response.toLowerCase().contains("no encontré") &&
            !MEDICAL_TERMINOLOGY_PATTERN.matcher(response).find()) {
            score -= 0.4;
        }
        
        // Verificar correspondencia con contexto médico
        String[] medicalTerms = {"médico", "salud", "clínico", "tratamiento", "diagnóstico", "paciente", "enfermedad"};
        int contextMatches = 0;
        String lowerContext = context.toLowerCase();
        String lowerResponse = response.toLowerCase();
        
        for (String term : medicalTerms) {
            if (lowerContext.contains(term) && lowerResponse.contains(term)) {
                contextMatches++;
            }
        }
        
        if (contextMatches > 0) {
            score += Math.min(0.3, contextMatches * 0.05);
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private boolean containsInventiveMedicalLanguage(String response) {
        String[] dangerousInventivePhrases = {
            "según mi experiencia médica", "como médico", "en mi práctica",
            "recomiendo tomar", "debe tomar", "no tome", "suspenda el medicamento",
            "diagnostico que", "padece de", "sufre de", "tiene la enfermedad"
        };
        
        String[] generalInventivePhrases = {
            "generalmente", "usualmente", "típicamente", "normalmente",
            "según mi conocimiento", "como se sabe", "es común que", "suele ser"
        };
        
        String lowerResponse = response.toLowerCase();
        
        // Verificar frases médicas peligrosas
        for (String phrase : dangerousInventivePhrases) {
            if (lowerResponse.contains(phrase)) {
                log.warn("Frase médica inventiva peligrosa detectada: '{}'", phrase);
                return true;
            }
        }
        
        // Verificar frases inventivas generales
        for (String phrase : generalInventivePhrases) {
            if (lowerResponse.contains(phrase)) {
                log.debug("Frase inventiva general detectada en respuesta médica: '{}'", phrase);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean needsSafetyDisclaimer(String response) {
        String[] sensitiveTopics = {
            "tratamiento", "medicamento", "fármaco", "dosis", "diagnóstico",
            "síntomas", "enfermedad", "dolor", "terapia", "cirugía"
        };
        
        String lowerResponse = response.toLowerCase();
        
        for (String topic : sensitiveTopics) {
            if (lowerResponse.contains(topic)) {
                return true;
            }
        }
        
        return false;
    }
}