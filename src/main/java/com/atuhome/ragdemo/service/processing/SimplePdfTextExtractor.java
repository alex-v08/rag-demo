package com.atuhome.ragdemo.service.processing;

import com.atuhome.ragdemo.exception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class SimplePdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(SimplePdfTextExtractor.class);

    public ExtractedText extractText(MultipartFile file) {
        log.info("Extrayendo texto de PDF: {}", file.getOriginalFilename());
        
        try (InputStream inputStream = file.getInputStream()) {
            PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
            
            PDFTextStripper textStripper = new PDFTextStripper();
            String extractedText = textStripper.getText(document);
            
            try {
                int pageCount = document.getNumberOfPages();
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", file.getOriginalFilename());
                metadata.put("size", file.getSize());
                metadata.put("simulated", false);
                metadata.put("pages", pageCount);
                
                ExtractedText result = ExtractedText.builder()
                        .content(extractedText)
                        .pageCount(pageCount)
                        .characterCount(extractedText.length())
                        .metadata(metadata)
                        .build();
                
                log.info("Texto extraído exitosamente: {} páginas, {} caracteres", 
                        result.getPageCount(), result.getCharacterCount());
                
                return result;
                
            } finally {
                document.close();
            }
            
        } catch (IOException e) {
            log.error("Error extrayendo texto del PDF: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error procesando PDF: " + e.getMessage(), e);
        }
    }


    public boolean validatePdf(MultipartFile file) {
        // Validación básica de archivos PDF
        if (file == null || file.isEmpty()) {
            log.warn("Archivo vacío o nulo: {}", file != null ? file.getOriginalFilename() : "null");
            return false;
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            log.warn("Archivo no es PDF: {}", filename);
            return false;
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB
            log.warn("Archivo demasiado grande: {} bytes", file.getSize());
            return false;
        }
        
        return true;
    }

    public static class ExtractedText {
        private final String content;
        private final int pageCount;
        private final int characterCount;
        private final Map<String, Object> metadata;

        private ExtractedText(Builder builder) {
            this.content = builder.content;
            this.pageCount = builder.pageCount;
            this.characterCount = builder.characterCount;
            this.metadata = builder.metadata;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getContent() { return content; }
        public int getPageCount() { return pageCount; }
        public int getCharacterCount() { return characterCount; }
        public Map<String, Object> getMetadata() { return metadata; }

        public static class Builder {
            private String content;
            private int pageCount;
            private int characterCount;
            private Map<String, Object> metadata;

            public Builder content(String content) {
                this.content = content;
                return this;
            }

            public Builder pageCount(int pageCount) {
                this.pageCount = pageCount;
                return this;
            }

            public Builder characterCount(int characterCount) {
                this.characterCount = characterCount;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public ExtractedText build() {
                return new ExtractedText(this);
            }
        }
    }
}