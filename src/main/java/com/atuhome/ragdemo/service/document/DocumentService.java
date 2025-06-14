package com.atuhome.ragdemo.service.document;

import com.atuhome.ragdemo.exception.DocumentProcessingException;
import com.atuhome.ragdemo.exception.ResourceNotFoundException;
import com.atuhome.ragdemo.model.dto.response.DocumentResponse;
import com.atuhome.ragdemo.model.entity.Document;
import com.atuhome.ragdemo.model.entity.DocumentChunk;
import com.atuhome.ragdemo.model.enums.DocumentStatus;
import com.atuhome.ragdemo.repository.DocumentRepository;
import com.atuhome.ragdemo.repository.DocumentChunkRepository;
import com.atuhome.ragdemo.service.ai.OllamaEmbeddingService;
import com.atuhome.ragdemo.service.processing.DocumentChunker;
import com.atuhome.ragdemo.service.processing.SimplePdfTextExtractor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;
    private final SimplePdfTextExtractor pdfTextExtractor;
    private final DocumentChunker documentChunker;
    private final OllamaEmbeddingService embeddingService;

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file) {
        log.info("Iniciando carga de documento: {}", file.getOriginalFilename());
        
        try {
            // Validar PDF
            if (!pdfTextExtractor.validatePdf(file)) {
                throw new DocumentProcessingException("El archivo PDF no es válido o no se puede procesar");
            }
            
            // Calcular hash para detectar duplicados
            String contentHash = fileStorageService.calculateFileHash(file);
            Optional<Document> existingDoc = documentRepository.findByContentHash(contentHash);
            
            if (existingDoc.isPresent()) {
                log.warn("Documento duplicado detectado: {}", file.getOriginalFilename());
                return mapToResponse(existingDoc.get(), "Documento ya existe en el sistema");
            }
            
            // Almacenar archivo
            String storedFilename = fileStorageService.storeFile(file);
            
            // Crear entidad Document
            Document document = Document.builder()
                    .filename(file.getOriginalFilename())
                    .filePath(storedFilename)
                    .fileSize(file.getSize())
                    .contentHash(contentHash)
                    .uploadDate(LocalDateTime.now())
                    .status(DocumentStatus.PENDING)
                    .build();
            
            document = documentRepository.save(document);
            
            // Procesar de forma asíncrona
            processDocumentAsync(document, file);
            
            log.info("Documento cargado exitosamente: {} (ID: {})", 
                    file.getOriginalFilename(), document.getId());
            
            return mapToResponse(document, "Documento cargado exitosamente");
            
        } catch (Exception e) {
            log.error("Error cargando documento: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error al cargar el documento", e);
        }
    }

    @Async("taskExecutor")
    public void processDocumentAsync(Document document, MultipartFile file) {
        try {
            processDocument(document, file);
        } catch (Exception e) {
            log.error("Error en procesamiento asíncrono del documento {}", document.getId(), e);
            markDocumentAsFailed(document.getId(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocument(Document document, MultipartFile file) {
        log.info("Iniciando procesamiento del documento: {}", document.getId());
        
        // Refrescar el documento desde la base de datos para evitar conflictos
        Document freshDocument = documentRepository.findById(document.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + document.getId()));
        
        try {
            // Actualizar estado a PROCESSING
            freshDocument.setStatus(DocumentStatus.PROCESSING);
            freshDocument.setProcessingStartedAt(LocalDateTime.now());
            documentRepository.save(freshDocument);
            
            // Extraer texto
            log.debug("Extrayendo texto del documento {}", freshDocument.getId());
            SimplePdfTextExtractor.ExtractedText extractedText = pdfTextExtractor.extractText(file);
            
            // Actualizar metadatos del documento
            freshDocument.setMetadata(extractedText.getMetadata());
            documentRepository.save(freshDocument);
            
            // Dividir en chunks
            log.debug("Dividiendo documento {} en chunks", freshDocument.getId());
            List<DocumentChunker.Chunk> chunks = documentChunker.chunkDocument(extractedText.getContent());
            
            // Guardar chunks
            log.debug("Guardando {} chunks para documento {}", chunks.size(), freshDocument.getId());
            List<DocumentChunk> documentChunks = chunks.stream()
                    .map(chunk -> DocumentChunk.builder()
                            .document(freshDocument)
                            .chunkIndex(chunk.getIndex())
                            .content(chunk.getContent())
                            .charStart(chunk.getCharStart())
                            .charEnd(chunk.getCharEnd())
                            .metadata(chunk.getMetadata())
                            .build())
                    .toList();
            
            chunkRepository.saveAll(documentChunks);
            
            // Generar embeddings
            log.debug("Generando embeddings para documento {}", freshDocument.getId());
            embeddingService.generateAndStoreEmbeddings(documentChunks);
            
            // Marcar como completado
            freshDocument.setStatus(DocumentStatus.COMPLETED);
            freshDocument.setProcessingCompletedAt(LocalDateTime.now());
            documentRepository.save(freshDocument);
            
            log.info("Procesamiento completado para documento: {} ({} chunks)", 
                    freshDocument.getId(), chunks.size());
            
        } catch (Exception e) {
            log.error("Error procesando documento {}", freshDocument.getId(), e);
            markDocumentAsFailed(freshDocument.getId(), e.getMessage());
            throw new DocumentProcessingException("Error en el procesamiento del documento", e);
        }
    }

    @Transactional
    public void markDocumentAsFailed(UUID documentId, String errorMessage) {
        Optional<Document> optionalDoc = documentRepository.findById(documentId);
        if (optionalDoc.isPresent()) {
            Document document = optionalDoc.get();
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(errorMessage);
            document.setProcessingCompletedAt(LocalDateTime.now());
            documentRepository.save(document);
            
            log.warn("Documento marcado como fallido: {} - Error: {}", documentId, errorMessage);
        }
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(doc -> mapToResponse(doc, null))
                .toList();
    }

    public Page<DocumentResponse> getDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable)
                .map(doc -> mapToResponse(doc, null));
    }

    public List<DocumentResponse> getDocumentsByStatus(DocumentStatus status) {
        return documentRepository.findByStatusOrderByUploadDateDesc(status).stream()
                .map(doc -> mapToResponse(doc, null))
                .toList();
    }

    public DocumentResponse getDocumentById(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + id));
        
        return mapToResponse(document, null);
    }

    @Transactional
    public void deleteDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + id));
        
        // Eliminar archivo físico
        if (document.getFilePath() != null) {
            fileStorageService.deleteFile(document.getFilePath());
        }
        
        // Eliminar de la base de datos (chunks se eliminan automáticamente por CASCADE)
        documentRepository.delete(document);
        
        log.info("Documento eliminado: {}", id);
    }

    @Transactional
    public void reprocessDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + id));
        
        if (document.getFilePath() == null) {
            throw new DocumentProcessingException("No se puede reprocesar: archivo no encontrado");
        }
        
        // Eliminar chunks existentes sin cargar embeddings
        chunkRepository.deleteByDocumentId(id);
        
        // Resetear estado
        document.setStatus(DocumentStatus.PENDING);
        document.setProcessingStartedAt(null);
        document.setProcessingCompletedAt(null);
        document.setErrorMessage(null);
        documentRepository.save(document);
        
        log.info("Documento marcado para reprocesamiento: {}", id);
    }

    public long getDocumentCount() {
        return documentRepository.count();
    }

    public long getDocumentCountByStatus(DocumentStatus status) {
        return documentRepository.countByStatus(status);
    }

    private DocumentResponse mapToResponse(Document document, String message) {
        long chunksCount = chunkRepository.countByDocumentId(document.getId());
        
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .uploadDate(document.getUploadDate())
                .processingStartedAt(document.getProcessingStartedAt())
                .processingCompletedAt(document.getProcessingCompletedAt())
                .errorMessage(document.getErrorMessage())
                .metadata(document.getMetadata())
                .chunksCount((int) chunksCount)
                .message(message)
                .build();
    }
}