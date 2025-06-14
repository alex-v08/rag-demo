# GUÍA DE DESARROLLO BACKEND
## MVP Sistema RAG Legal - Spring Boot + Ollama

---

## 1. CHECKLIST DE TAREAS - PASO A PASO

### 📋 Fase 1: Setup Inicial
- [ ] Crear proyecto Spring Boot con Spring Initializr
- [ ] Configurar estructura de paquetes
- [ ] Agregar dependencias en pom.xml
- [ ] Crear application.yml con perfiles
- [ ] Configurar .gitignore
- [ ] Setup PostgreSQL local con pgvector
- [ ] Instalar y configurar Ollama
- [ ] Descargar modelos necesarios
- [ ] Crear esquema de base de datos
- [ ] Verificar conectividad con todos los servicios

### 📋 Fase 2: Capa de Dominio
- [ ] Crear entidad Document
- [ ] Crear entidad DocumentChunk
- [ ] Crear entidad QAHistory
- [ ] Definir DTOs para request/response
- [ ] Crear enums para estados
- [ ] Implementar validaciones básicas
- [ ] Crear excepciones personalizadas

### 📋 Fase 3: Capa de Persistencia
- [ ] Crear DocumentRepository
- [ ] Crear DocumentChunkRepository
- [ ] Crear QAHistoryRepository
- [ ] Implementar queries nativas para búsqueda vectorial
- [ ] Crear índices en PostgreSQL
- [ ] Implementar paginación
- [ ] Tests de repositorios

### 📋 Fase 4: Servicios Core
- [ ] Implementar FileStorageService
- [ ] Implementar PdfTextExtractor
- [ ] Implementar DocumentChunker
- [ ] Implementar EmbeddingService
- [ ] Configurar OllamaService
- [ ] Implementar SemanticSearchService
- [ ] Crear ContextBuilder
- [ ] Implementar AntiHallucinationService

### 📋 Fase 5: Lógica de Negocio
- [ ] Crear DocumentService
- [ ] Implementar pipeline de procesamiento
- [ ] Crear RagService
- [ ] Implementar flujo completo Q&A
- [ ] Agregar manejo de errores
- [ ] Implementar logging
- [ ] Agregar métricas básicas

### 📋 Fase 6: API REST
- [ ] Crear DocumentController
- [ ] Crear QAController
- [ ] Implementar HealthCheckController
- [ ] Configurar CORS
- [ ] Agregar documentación OpenAPI
- [ ] Implementar manejo global de excepciones
- [ ] Crear filtros de seguridad básicos

### 📋 Fase 7: Testing
- [ ] Tests unitarios para servicios
- [ ] Tests de integración para repositorios
- [ ] Tests de controladores con MockMvc
- [ ] Tests E2E básicos
- [ ] Configurar Testcontainers
- [ ] Coverage mínimo 70%

### 📋 Fase 8: Deployment
- [ ] Crear Dockerfile multi-stage
- [ ] Configurar docker-compose.yml
- [ ] Scripts de inicialización
- [ ] Documentación de API
- [ ] README con instrucciones
- [ ] Health checks y readiness probes

---

## 2. ESTRUCTURA DE CAPAS DEL PROYECTO

```
src/main/java/com/legalrag/mvp/
│
├── 📁 config/
│   ├── OllamaConfig.java
│   ├── DatabaseConfig.java
│   ├── WebConfig.java
│   └── ApplicationConfig.java
│
├── 📁 controller/
│   ├── DocumentController.java
│   ├── QAController.java
│   ├── HealthController.java
│   └── advice/
│       └── GlobalExceptionHandler.java
│
├── 📁 service/
│   ├── document/
│   │   ├── DocumentService.java
│   │   ├── FileStorageService.java
│   │   └── DocumentProcessingService.java
│   │
│   ├── ai/
│   │   ├── EmbeddingService.java
│   │   ├── OllamaService.java
│   │   └── AntiHallucinationService.java
│   │
│   ├── rag/
│   │   ├── RagService.java
│   │   ├── SemanticSearchService.java
│   │   └── ContextBuilderService.java
│   │
│   └── processing/
│       ├── PdfTextExtractor.java
│       └── DocumentChunker.java
│
├── 📁 repository/
│   ├── DocumentRepository.java
│   ├── DocumentChunkRepository.java
│   └── QAHistoryRepository.java
│
├── 📁 model/
│   ├── entity/
│   │   ├── Document.java
│   │   ├── DocumentChunk.java
│   │   └── QAHistory.java
│   │
│   ├── dto/
│   │   ├── request/
│   │   │   ├── QuestionRequest.java
│   │   │   └── DocumentUploadRequest.java
│   │   │
│   │   └── response/
│   │       ├── AnswerResponse.java
│   │       ├── DocumentResponse.java
│   │       └── SearchResult.java
│   │
│   └── enums/
│       ├── DocumentStatus.java
│       └── ProcessingStatus.java
│
├── 📁 exception/
│   ├── DocumentProcessingException.java
│   ├── RagException.java
│   └── ResourceNotFoundException.java
│
└── 📁 util/
    ├── Constants.java
    ├── TextUtils.java
    └── EmbeddingUtils.java
```

---

## 3. DEPENDENCIAS MAVEN (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>
    
    <groupId>com.legalrag</groupId>
    <artifactId>legal-rag-mvp</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>legal-rag-mvp</name>
    <description>MVP RAG System for Legal Documents</description>
    
    <properties>
        <java.version>21</java.version>
        <spring-ai.version>0.8.1</spring-ai.version>
        <pgvector.version>0.1.4</pgvector.version>
        <pdfbox.version>3.0.1</pdfbox.version>
        <testcontainers.version>1.19.5</testcontainers.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-transformers-spring-boot-starter</artifactId>
            <version>${spring-ai.version}</version>
        </dependency>
        
        <!-- PostgreSQL y pgvector -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>com.pgvector</groupId>
            <artifactId>pgvector</artifactId>
            <version>${pgvector.version}</version>
        </dependency>
        
        <!-- PDF Processing -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>${pdfbox.version}</version>
        </dependency>
        
        <!-- Utils -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.15.1</version>
        </dependency>
        
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        
        <!-- OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-test</artifactId>
            <version>${spring-ai.version}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
    
    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

---

## 4. CONFIGURACIÓN BASE (application.yml)

```yaml
spring:
  application:
    name: legal-rag-mvp
    
  profiles:
    active: dev
    
  datasource:
    url: jdbc:postgresql://localhost:5432/legal_rag
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          lob:
            non_contextual_creation: true
    show-sql: false
    
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${OLLAMA_MODEL:deepseek-r1:latest}
          temperature: 0.1
          top-p: 0.9
          num-ctx: 4096
          num-predict: 2048
          
    embedding:
      transformer:
        onnx:
          model-uri: ${EMBEDDING_MODEL:sentence-transformers/all-MiniLM-L6-v2}
          
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB
      
logging:
  level:
    com.legalrag: DEBUG
    org.springframework.ai: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    
# Application specific properties
app:
  storage:
    upload-dir: ${UPLOAD_DIR:./uploads}
    
  rag:
    chunk:
      size: 1000
      overlap: 200
      
    search:
      similarity-threshold: 0.7
      max-results: 5
      
    embedding:
      dimension: 384  # for all-MiniLM-L6-v2
      batch-size: 10
      
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:8080}
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

---

## 5. SCRIPTS SQL INICIALES

```sql
-- init.sql
-- Crear extensión pgvector
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla de documentos
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_hash VARCHAR(64),
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de chunks
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(384), -- Dimensión para all-MiniLM-L6-v2
    char_start INTEGER,
    char_end INTEGER,
    page_number INTEGER,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- Tabla de historial Q&A
CREATE TABLE IF NOT EXISTS qa_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question TEXT NOT NULL,
    answer TEXT,
    context_used TEXT,
    sources JSONB,
    model_used VARCHAR(100),
    response_time_ms INTEGER,
    feedback_rating INTEGER CHECK (feedback_rating >= 1 AND feedback_rating <= 5),
    feedback_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_filename ON documents(filename);
CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
CREATE INDEX idx_qa_history_created ON qa_history(created_at DESC);

-- Función para actualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para updated_at
CREATE TRIGGER update_documents_updated_at BEFORE UPDATE
    ON documents FOR EACH ROW EXECUTE FUNCTION 
    update_updated_at_column();
```

---

## 6. CLASES CORE ESENCIALES

### 6.1 Entidades JPA

```java
// Document.java
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String filename;
    
    private String filePath;
    private Long fileSize;
    private String contentHash;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;
    
    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private List<DocumentChunk> chunks;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

// DocumentChunk.java
@Entity
@Table(name = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(nullable = false)
    private Integer chunkIndex;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "vector(384)")
    private float[] embedding;
    
    private Integer charStart;
    private Integer charEnd;
    private Integer pageNumber;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 6.2 Configuración Ollama

```java
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {
    
    @Bean
    public OllamaApi ollamaApi(OllamaProperties properties) {
        return new OllamaApi(properties.getBaseUrl());
    }
    
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi, 
                                            OllamaProperties properties) {
        return new OllamaChatClient(ollamaApi,
            OllamaOptions.create()
                .withModel(properties.getModel())
                .withTemperature(properties.getTemperature())
                .withNumCtx(properties.getNumCtx())
        );
    }
}

@ConfigurationProperties(prefix = "app.ai.ollama")
@Data
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String model = "deepseek-r1:latest";
    private Float temperature = 0.1f;
    private Integer numCtx = 4096;
}
```

### 6.3 Servicio Principal RAG

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {
    
    private final EmbeddingService embeddingService;
    private final SemanticSearchService searchService;
    private final ContextBuilderService contextBuilder;
    private final OllamaChatClient chatClient;
    private final AntiHallucinationService antiHallucinationService;
    
    public AnswerResponse processQuestion(String question) {
        log.info("Processing question: {}", question);
        
        try {
            // 1. Generate embedding for question
            List<Float> queryEmbedding = embeddingService.generateEmbedding(question);
            
            // 2. Semantic search
            List<SearchResult> searchResults = searchService.findSimilarChunks(
                queryEmbedding, 
                0.7f, 
                5
            );
            
            if (searchResults.isEmpty()) {
                return AnswerResponse.builder()
                    .question(question)
                    .answer("No encontré información relevante en los documentos disponibles.")
                    .sources(Collections.emptyList())
                    .build();
            }
            
            // 3. Build context
            String context = contextBuilder.buildContext(searchResults);
            
            // 4. Create prompt with anti-hallucination
            String prompt = antiHallucinationService.createStrictPrompt(question, context);
            
            // 5. Generate answer
            String answer = chatClient.call(prompt);
            
            // 6. Validate response
            if (!antiHallucinationService.validateResponse(answer)) {
                log.warn("Response failed validation, using fallback");
                answer = "No puedo proporcionar una respuesta confiable basada en los documentos disponibles.";
            }
            
            return AnswerResponse.builder()
                .question(question)
                .answer(answer)
                .sources(searchResults)
                .responseTime(System.currentTimeMillis())
                .build();
                
        } catch (Exception e) {
            log.error("Error processing question", e);
            throw new RagException("Error al procesar la pregunta", e);
        }
    }
}
```

---

## 7. COMANDOS DE DESARROLLO

```bash
# 1. Clonar y setup inicial
git clone <repo>
cd legal-rag-mvp
mvn clean install

# 2. Levantar PostgreSQL con pgvector
docker run -d \
  --name postgres-pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=legal_rag \
  -p 5432:5432 \
  ankane/pgvector:latest

# 3. Instalar y configurar Ollama
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull deepseek-r1:latest
ollama pull all-minilm-l6-v2

# 4. Ejecutar aplicación
mvn spring-boot:run

# 5. Build para producción
mvn clean package -DskipTests
java -jar target/legal-rag-mvp-0.0.1-SNAPSHOT.jar

# 6. Ejecutar tests
mvn test

# 7. Generar reporte de cobertura
mvn jacoco:report
```

---

## 8. ENDPOINTS PRINCIPALES

```yaml
# Document Upload
POST /api/documents/upload
Content-Type: multipart/form-data
Body: file (PDF)

# List Documents
GET /api/documents
Response: List<DocumentResponse>

# Ask Question
POST /api/qa/ask
Content-Type: application/json
Body: {
  "question": "¿Cuál es el plazo para...?"
}

# Health Check
GET /actuator/health

# API Documentation
GET /swagger-ui.html
```

---

## 9. ESTRUCTURA DE RESPUESTA

```json
// Document Upload Response
{
  "id": "uuid",
  "filename": "documento.pdf",
  "status": "PROCESSING",
  "uploadDate": "2024-02-20T10:30:00",
  "message": "Documento cargado exitosamente"
}

// Q&A Response
{
  "question": "¿Cuál es el plazo para presentar la demanda?",
  "answer": "Según el documento [Código Procesal, Chunk 3], el plazo para presentar la demanda es de 30 días hábiles...",
  "sources": [
    {
      "documentName": "codigo_procesal.pdf",
      "chunkIndex": 3,
      "similarity": 0.89,
      "content": "El plazo para presentar..."
    }
  ],
  "responseTime": 2341,
  "timestamp": "2024-02-20T10:35:00"
}
```

---

## 10. TROUBLESHOOTING COMÚN

| Problema | Solución |
|----------|----------|
| `pg_vector extension not found` | Usar imagen Docker `ankane/pgvector` |
| `Ollama connection refused` | Verificar que Ollama esté corriendo: `systemctl status ollama` |
| `Out of memory con embeddings` | Reducir batch size o aumentar heap de JVM |
| `Modelo no encontrado` | Ejecutar `ollama pull deepseek-r1:latest` |
| `Timeout en respuestas` | Aumentar `num-ctx` en configuración Ollama |

---

**Nota:** Este documento contiene todo lo necesario para implementar el backend del MVP RAG. Seguir las tareas en orden garantiza una implementación exitosa.

