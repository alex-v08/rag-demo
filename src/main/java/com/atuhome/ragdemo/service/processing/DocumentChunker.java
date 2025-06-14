package com.atuhome.ragdemo.service.processing;

import com.atuhome.ragdemo.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    private final RagProperties ragProperties;

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n\n+");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z])");
    private static final Pattern LEGAL_SECTION_PATTERN = Pattern.compile(
        "(?i)(artículo|art\\.|capítulo|cap\\.|sección|sec\\.|título|tít\\.)\\s*\\d+"
    );

    public List<Chunk> chunkDocument(String content) {
        int maxChunkSize = ragProperties.getChunk().getSize();
        int overlapSize = ragProperties.getChunk().getOverlap();
        
        log.info("Dividiendo documento en chunks: tamaño máximo={}, overlap={}", maxChunkSize, overlapSize);
        
        if (content == null || content.trim().isEmpty()) {
            log.warn("Contenido vacío para chunking");
            return List.of();
        }
        
        List<Chunk> chunks = new ArrayList<>();
        
        // Dividir por párrafos primero
        String[] paragraphs = PARAGRAPH_PATTERN.split(content);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int currentPosition = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            // Si el párrafo por sí solo es muy grande, dividirlo
            if (paragraph.length() > maxChunkSize) {
                // Guardar chunk actual si tiene contenido
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), chunkIndex++, 
                                         currentPosition, currentPosition + currentChunk.length()));
                    currentChunk = new StringBuilder();
                }
                
                // Dividir párrafo grande
                List<Chunk> subChunks = splitLargeParagraph(paragraph, chunkIndex, 
                                                          currentPosition, maxChunkSize, overlapSize);
                chunks.addAll(subChunks);
                chunkIndex += subChunks.size();
                currentPosition += paragraph.length();
                continue;
            }
            
            // Verificar si agregar este párrafo excedería el tamaño máximo
            if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize && currentChunk.length() > 0) {
                // Guardar chunk actual
                String chunkContent = currentChunk.toString();
                chunks.add(createChunk(chunkContent, chunkIndex++, 
                                     currentPosition - chunkContent.length(), currentPosition));
                
                // Iniciar nuevo chunk con overlap si es posible
                currentChunk = new StringBuilder();
                if (chunkContent.length() > overlapSize) {
                    String overlap = getLastWords(chunkContent, overlapSize);
                    currentChunk.append(overlap).append("\n\n");
                }
            }
            
            // Agregar párrafo al chunk actual
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
            currentPosition += paragraph.length() + 2; // +2 por los saltos de línea
        }
        
        // Agregar último chunk si tiene contenido
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), chunkIndex, 
                                 currentPosition - currentChunk.length(), currentPosition));
        }
        
        log.info("Documento dividido en {} chunks", chunks.size());
        return chunks;
    }

    private List<Chunk> splitLargeParagraph(String paragraph, int startIndex, int startPosition, 
                                          int maxChunkSize, int overlapSize) {
        List<Chunk> chunks = new ArrayList<>();
        
        // Intentar dividir por oraciones primero
        String[] sentences = SENTENCE_PATTERN.split(paragraph);
        
        if (sentences.length == 1) {
            // Si es una sola oración muy larga, dividir por caracteres
            return splitByCharacters(paragraph, startIndex, startPosition, maxChunkSize, overlapSize);
        }
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = startIndex;
        int currentPosition = startPosition;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            // Si la oración sola es muy grande, dividir por caracteres
            if (sentence.length() > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), chunkIndex++, 
                                         currentPosition - currentChunk.length(), currentPosition));
                    currentChunk = new StringBuilder();
                }
                
                chunks.addAll(splitByCharacters(sentence, chunkIndex, currentPosition, 
                                              maxChunkSize, overlapSize));
                chunkIndex += chunks.size() - chunkIndex;
                currentPosition += sentence.length();
                continue;
            }
            
            // Verificar si agregar esta oración excedería el tamaño
            if (currentChunk.length() + sentence.length() + 1 > maxChunkSize && currentChunk.length() > 0) {
                String chunkContent = currentChunk.toString();
                chunks.add(createChunk(chunkContent, chunkIndex++, 
                                     currentPosition - chunkContent.length(), currentPosition));
                
                currentChunk = new StringBuilder();
                if (chunkContent.length() > overlapSize) {
                    String overlap = getLastWords(chunkContent, overlapSize);
                    currentChunk.append(overlap).append(" ");
                }
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            currentPosition += sentence.length() + 1;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), chunkIndex, 
                                 currentPosition - currentChunk.length(), currentPosition));
        }
        
        return chunks;
    }

    private List<Chunk> splitByCharacters(String text, int startIndex, int startPosition, 
                                        int maxChunkSize, int overlapSize) {
        List<Chunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < text.length(); i += maxChunkSize - overlapSize) {
            int end = Math.min(i + maxChunkSize, text.length());
            String chunkContent = text.substring(i, end);
            
            chunks.add(createChunk(chunkContent, startIndex + chunks.size(), 
                                 startPosition + i, startPosition + end));
        }
        
        return chunks;
    }

    private String getLastWords(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        
        String substring = text.substring(text.length() - maxLength);
        
        // Buscar el primer espacio para no cortar palabras
        int firstSpace = substring.indexOf(' ');
        if (firstSpace > 0 && firstSpace < substring.length() - 1) {
            return substring.substring(firstSpace + 1);
        }
        
        return substring;
    }

    private Chunk createChunk(String content, int index, int charStart, int charEnd) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Detectar si contiene secciones legales
        if (LEGAL_SECTION_PATTERN.matcher(content).find()) {
            metadata.put("containsLegalSection", true);
        }
        
        // Agregar estadísticas básicas
        metadata.put("wordCount", content.split("\\s+").length);
        metadata.put("characterCount", content.length());
        
        return Chunk.builder()
                .content(content.trim())
                .index(index)
                .charStart(charStart)
                .charEnd(charEnd)
                .metadata(metadata)
                .build();
    }

    public static class Chunk {
        private final String content;
        private final int index;
        private final int charStart;
        private final int charEnd;
        private final Map<String, Object> metadata;

        private Chunk(Builder builder) {
            this.content = builder.content;
            this.index = builder.index;
            this.charStart = builder.charStart;
            this.charEnd = builder.charEnd;
            this.metadata = builder.metadata;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getContent() { return content; }
        public int getIndex() { return index; }
        public int getCharStart() { return charStart; }
        public int getCharEnd() { return charEnd; }
        public Map<String, Object> getMetadata() { return metadata; }

        public static class Builder {
            private String content;
            private int index;
            private int charStart;
            private int charEnd;
            private Map<String, Object> metadata;

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder index(int index) {
                this.index = index;
                return this;
            }

            public Builder charStart(int charStart) {
                this.charStart = charStart;
                return this;
            }

            public Builder charEnd(int charEnd) {
                this.charEnd = charEnd;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public Chunk build() {
                return new Chunk(this);
            }
        }
    }
}