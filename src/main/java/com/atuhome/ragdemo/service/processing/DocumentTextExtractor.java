package com.atuhome.ragdemo.service.processing;

import com.atuhome.ragdemo.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    @Autowired
    private SimplePdfTextExtractor pdfExtractor;

    public SimplePdfTextExtractor.ExtractedText extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new DocumentProcessingException("Nombre de archivo no válido");
        }

        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case "pdf":
                return extractPdfText(file);
            case "txt":
                return extractPlainText(file);
            case "md":
                return extractMarkdownText(file);
            default:
                throw new DocumentProcessingException(
                    "Tipo de archivo no soportado: " + extension + 
                    ". Formatos soportados: PDF, TXT, MD");
        }
    }

    public boolean validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Archivo vacío o nulo: {}", file != null ? file.getOriginalFilename() : "null");
            return false;
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            log.warn("Nombre de archivo nulo");
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!isSupportedFormat(extension)) {
            log.warn("Formato no soportado: {}", extension);
            return false;
        }
        
        if (file.getSize() > 50 * 1024 * 1024) { // 50MB
            log.warn("Archivo demasiado grande: {} bytes", file.getSize());
            return false;
        }
        
        return true;
    }

    private boolean isSupportedFormat(String extension) {
        return extension.equals("pdf") || extension.equals("txt") || extension.equals("md");
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private SimplePdfTextExtractor.ExtractedText extractPdfText(MultipartFile file) {
        if (!pdfExtractor.validatePdf(file)) {
            throw new DocumentProcessingException("El archivo PDF no es válido o no se puede procesar");
        }
        return pdfExtractor.extractText(file);
    }

    private SimplePdfTextExtractor.ExtractedText extractPlainText(MultipartFile file) {
        log.info("Extrayendo texto de archivo TXT: {}", file.getOriginalFilename());
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            String extractedText = content.toString();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("size", file.getSize());
            metadata.put("format", "txt");
            metadata.put("encoding", "UTF-8");
            
            SimplePdfTextExtractor.ExtractedText result = SimplePdfTextExtractor.ExtractedText.builder()
                    .content(extractedText)
                    .pageCount(1) // Los archivos de texto se consideran como 1 página
                    .characterCount(extractedText.length())
                    .metadata(metadata)
                    .build();
            
            log.info("Texto extraído exitosamente: {} caracteres", result.getCharacterCount());
            return result;
            
        } catch (IOException e) {
            log.error("Error extrayendo texto del archivo TXT: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error procesando archivo TXT: " + e.getMessage(), e);
        }
    }

    private SimplePdfTextExtractor.ExtractedText extractMarkdownText(MultipartFile file) {
        log.info("Extrayendo texto de archivo Markdown: {}", file.getOriginalFilename());
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            String extractedText = content.toString();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("size", file.getSize());
            metadata.put("format", "markdown");
            metadata.put("encoding", "UTF-8");
            
            SimplePdfTextExtractor.ExtractedText result = SimplePdfTextExtractor.ExtractedText.builder()
                    .content(extractedText)
                    .pageCount(1) // Los archivos Markdown se consideran como 1 página
                    .characterCount(extractedText.length())
                    .metadata(metadata)
                    .build();
            
            log.info("Texto extraído exitosamente: {} caracteres", result.getCharacterCount());
            return result;
            
        } catch (IOException e) {
            log.error("Error extrayendo texto del archivo Markdown: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error procesando archivo Markdown: " + e.getMessage(), e);
        }
    }
}