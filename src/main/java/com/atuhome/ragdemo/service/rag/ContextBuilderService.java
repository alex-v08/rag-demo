package com.atuhome.ragdemo.service.rag;

import com.atuhome.ragdemo.model.dto.response.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContextBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilderService.class);

    private static final int MAX_CONTEXT_LENGTH = 3000; // Límite de caracteres para el contexto

    public String buildContext(List<SearchResult> searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            log.debug("Lista de resultados vacía para construcción de contexto");
            return "No hay contexto disponible.";
        }

        log.debug("Construyendo contexto con {} resultados", searchResults.size());

        StringBuilder contextBuilder = new StringBuilder();
        int totalLength = 0;

        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = searchResults.get(i);
            
            // Formatear el chunk con información de origen
            String formattedChunk = formatChunk(result, i + 1);
            
            // Verificar si agregar este chunk excedería el límite
            if (totalLength + formattedChunk.length() > MAX_CONTEXT_LENGTH) {
                log.debug("Límite de contexto alcanzado. Usando {} de {} chunks", 
                         i, searchResults.size());
                break;
            }
            
            contextBuilder.append(formattedChunk);
            totalLength += formattedChunk.length();
            
            // Agregar separador entre chunks (excepto el último)
            if (i < searchResults.size() - 1) {
                contextBuilder.append("\n---\n");
                totalLength += 5; // longitud del separador
            }
        }

        String finalContext = contextBuilder.toString().trim();
        
        log.debug("Contexto construido: {} caracteres, {} chunks utilizados", 
                 finalContext.length(), countChunksUsed(finalContext));
        
        return finalContext;
    }

    public String buildContextWithSimilarity(List<SearchResult> searchResults, 
                                           double minSimilarity) {
        List<SearchResult> filteredResults = searchResults.stream()
                .filter(result -> result.getSimilarity() >= minSimilarity)
                .toList();
        
        log.debug("Filtrando por similitud >= {}: {} -> {} chunks", 
                 minSimilarity, searchResults.size(), filteredResults.size());
        
        return buildContext(filteredResults);
    }

    public String buildContextWithMaxChunks(List<SearchResult> searchResults, int maxChunks) {
        List<SearchResult> limitedResults = searchResults.stream()
                .limit(maxChunks)
                .toList();
        
        log.debug("Limitando a {} chunks: {} -> {} chunks", 
                 maxChunks, searchResults.size(), limitedResults.size());
        
        return buildContext(limitedResults);
    }

    private String formatChunk(SearchResult result, int position) {
        StringBuilder chunkBuilder = new StringBuilder();
        
        // Encabezado del chunk
        chunkBuilder.append(String.format("FRAGMENTO %d:\n", position));
        chunkBuilder.append(String.format("Fuente: %s\n", result.getDocumentName()));
        
        if (result.getPageNumber() != null) {
            chunkBuilder.append(String.format("Página: %d\n", result.getPageNumber()));
        }
        
        chunkBuilder.append(String.format("Similitud: %.2f\n", result.getSimilarity()));
        chunkBuilder.append("\nContenido:\n");
        
        // Contenido del chunk
        String content = result.getContent();
        if (content != null) {
            // Limpiar y formatear el contenido
            content = content.trim();
            content = normalizeWhitespace(content);
            chunkBuilder.append(content);
        } else {
            chunkBuilder.append("[Contenido no disponible]");
        }
        
        chunkBuilder.append("\n");
        
        return chunkBuilder.toString();
    }

    private String normalizeWhitespace(String text) {
        // Normalizar espacios en blanco y saltos de línea
        return text.replaceAll("\\s+", " ")
                  .replaceAll("\\. ", ".\\n")
                  .trim();
    }

    private int countChunksUsed(String context) {
        if (context == null || context.isEmpty()) {
            return 0;
        }
        
        // Contar los fragmentos por el patrón "FRAGMENTO X:"
        return (int) context.lines()
                .filter(line -> line.matches("FRAGMENTO \\d+:"))
                .count();
    }

    public ContextMetadata analyzeContext(String context) {
        if (context == null || context.isEmpty()) {
            return new ContextMetadata(0, 0, 0, 0.0);
        }

        int characterCount = context.length();
        int wordCount = context.split("\\s+").length;
        int chunkCount = countChunksUsed(context);
        
        // Calcular densidad de información (aproximada)
        double informationDensity = chunkCount > 0 ? (double) wordCount / chunkCount : 0.0;

        return new ContextMetadata(characterCount, wordCount, chunkCount, informationDensity);
    }

    public String summarizeContext(String context, int maxSummaryLength) {
        if (context == null || context.length() <= maxSummaryLength) {
            return context;
        }

        log.debug("Resumiendo contexto de {} a {} caracteres", 
                 context.length(), maxSummaryLength);

        // Estrategia simple: tomar los primeros fragmentos completos que quepan
        String[] fragments = context.split("FRAGMENTO \\d+:");
        StringBuilder summary = new StringBuilder();
        
        for (String fragment : fragments) {
            if (fragment.trim().isEmpty()) continue;
            
            String fragmentWithHeader = "FRAGMENTO " + (summary.length() > 0 ? "..." : "1") + ":" + fragment;
            
            if (summary.length() + fragmentWithHeader.length() <= maxSummaryLength) {
                summary.append(fragmentWithHeader);
            } else {
                break;
            }
        }

        String result = summary.toString();
        if (result.length() > maxSummaryLength) {
            result = result.substring(0, maxSummaryLength - 3) + "...";
        }

        return result;
    }

    public static class ContextMetadata {
        private final int characterCount;
        private final int wordCount;
        private final int chunkCount;
        private final double informationDensity;

        public ContextMetadata(int characterCount, int wordCount, int chunkCount, double informationDensity) {
            this.characterCount = characterCount;
            this.wordCount = wordCount;
            this.chunkCount = chunkCount;
            this.informationDensity = informationDensity;
        }

        public int getCharacterCount() { return characterCount; }
        public int getWordCount() { return wordCount; }
        public int getChunkCount() { return chunkCount; }
        public double getInformationDensity() { return informationDensity; }

        @Override
        public String toString() {
            return String.format("ContextMetadata{chars=%d, words=%d, chunks=%d, density=%.2f}", 
                               characterCount, wordCount, chunkCount, informationDensity);
        }
    }
}