package com.atuhome.ragdemo.service.document;

import com.atuhome.ragdemo.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Directorio de uploads creado/verificado: {}", uploadDir);
        } catch (IOException e) {
            log.error("Error creando directorio de uploads: {}", uploadDir, e);
            throw new DocumentProcessingException("No se pudo crear el directorio de uploads", e);
        }
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);
        
        try {
            // Generar nombre único para el archivo
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            
            // Resolver la ruta completa
            Path targetLocation = uploadDir.resolve(uniqueFilename);
            
            // Verificar que la ruta esté dentro del directorio permitido
            if (!targetLocation.getParent().equals(uploadDir)) {
                throw new DocumentProcessingException("Ruta de archivo inválida");
            }
            
            // Copiar archivo
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Archivo almacenado: {} -> {}", originalFilename, uniqueFilename);
            return uniqueFilename;
            
        } catch (IOException e) {
            log.error("Error almacenando archivo: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error al almacenar el archivo", e);
        }
    }

    public Path getFilePath(String filename) {
        Path filePath = uploadDir.resolve(filename);
        
        if (!filePath.getParent().equals(uploadDir)) {
            throw new DocumentProcessingException("Ruta de archivo inválida: " + filename);
        }
        
        if (!Files.exists(filePath)) {
            throw new DocumentProcessingException("Archivo no encontrado: " + filename);
        }
        
        return filePath;
    }

    public boolean deleteFile(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename);
            
            if (!filePath.getParent().equals(uploadDir)) {
                log.warn("Intento de eliminar archivo fuera del directorio permitido: {}", filename);
                return false;
            }
            
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Archivo eliminado: {}", filename);
            } else {
                log.warn("Archivo no encontrado para eliminar: {}", filename);
            }
            
            return deleted;
            
        } catch (IOException e) {
            log.error("Error eliminando archivo: {}", filename, e);
            return false;
        }
    }

    public String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Error calculando hash del archivo: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Error al calcular hash del archivo", e);
        }
    }

    public boolean fileExists(String filename) {
        Path filePath = uploadDir.resolve(filename);
        return Files.exists(filePath) && filePath.getParent().equals(uploadDir);
    }

    public long getFileSize(String filename) {
        try {
            Path filePath = getFilePath(filename);
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Error obteniendo tamaño del archivo: {}", filename, e);
            return 0;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DocumentProcessingException("El archivo está vacío");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new DocumentProcessingException("Nombre de archivo inválido");
        }
        
        // Verificar extensión PDF
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new DocumentProcessingException("Solo se permiten archivos PDF");
        }
        
        // Verificar tamaño (50MB máximo)
        long maxSize = 50 * 1024 * 1024; // 50MB en bytes
        if (file.getSize() > maxSize) {
            throw new DocumentProcessingException("El archivo excede el tamaño máximo permitido (50MB)");
        }
        
        // Verificar caracteres peligrosos en el nombre
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new DocumentProcessingException("Nombre de archivo contiene caracteres no permitidos");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex);
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}