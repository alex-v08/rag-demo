# RAG Demo - Sistema de Recuperación y Generación Aumentada

Un sistema completo de RAG (Retrieval-Augmented Generation) construido con Spring Boot 3.5 y Java 21, que utiliza Ollama para modelos de lenguaje local y PostgreSQL con pgvector para almacenamiento y búsqueda de vectores.

## 📖 Índice

- [Características Actuales](#-características-actuales)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Uso del Sistema](#-uso-del-sistema)
- [Nuevas Características Propuestas](#-nuevas-características-propuestas)
- [Mejoras Potenciales](#-mejoras-potenciales)
- [API Endpoints](#-api-endpoints)
- [Contribuir al Proyecto](#-contribuir-al-proyecto)

## 🚀 Características Actuales

### 1. **Gestión de Documentos**
- **Carga de documentos**: Soporte para PDF y archivos de texto
- **Procesamiento automático**: Extracción de texto y división en chunks
- **Almacenamiento**: Base de datos PostgreSQL con metadatos completos
- **Estado de procesamiento**: Seguimiento del estado de cada documento

**Implementación clave:**
```java
@Service
public class DocumentService {
    public DocumentResponse uploadDocument(MultipartFile file) {
        // Validación, almacenamiento y procesamiento asíncrono
    }
}
```

### 2. **Sistema de Embeddings Vectoriales**
- **Modelo**: BGE-M3 (1024 dimensiones) a través de Ollama
- **Almacenamiento**: PostgreSQL con extensión pgvector
- **Búsqueda semántica**: Cosine similarity para encontrar chunks relevantes
- **Procesamiento asíncrono**: Generación de embeddings en background

**Configuración:**
```properties
spring.ai.ollama.embedding.options.model=bge-m3:latest
app.rag.embedding.dimension=1024
app.rag.search.similarity-threshold=0.2
```

### 3. **Motor de Búsqueda Semántica**
- **Algoritmo**: Búsqueda por similitud de coseno
- **Filtros configurables**: Umbral de similitud y número máximo de resultados
- **Ranking de resultados**: Ordenamiento por relevancia
- **Contexto enriquecido**: Metadatos de documentos incluidos

**Características:**
- Threshold de similitud ajustable (default: 0.2)
- Máximo 5 resultados por defecto
- Información de chunk, documento y página

### 4. **Sistema de Respuestas con LLM**
- **Modelos soportados**: Todos los modelos disponibles en Ollama local
- **Cambio dinámico**: API para cambiar modelos en tiempo real
- **Prompts optimizados**: Sistema de prompts balanceado para respuestas precisas
- **Anti-alucinación**: Validación de respuestas y fallbacks

**Modelos disponibles:**
- llama3.2:latest (default)
- deepseek-r1:latest
- Cualquier modelo instalado en Ollama

### 5. **API RESTful Completa**
- **Swagger UI**: Documentación interactiva en `/swagger-ui.html`
- **Endpoints organizados**: Separados por funcionalidad
- **Manejo de errores**: Respuestas estructuradas y logging detallado
- **CORS configurado**: Soporte para frontend

### 6. **Gestión de Modelos Dinámicos**
- **Lista de modelos**: Obtener todos los modelos disponibles en Ollama
- **Cambio en tiempo real**: Switching entre modelos sin reiniciar
- **Información de modelos**: Tamaño, fecha de modificación, estado activo
- **Filtrado inteligente**: Separación entre modelos de chat y embeddings

### 7. **Monitorización y Health Checks**
- **Spring Actuator**: Endpoints de salud y métricas
- **Logging detallado**: Múltiples niveles configurables
- **Estadísticas del sistema**: Conteo de documentos, chunks y tiempo de respuesta
- **Métricas de rendimiento**: Tiempo de procesamiento por operación

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend/     │    │   Spring Boot    │    │   PostgreSQL    │
│   API Client    │◄──►│   Application    │◄──►│   + pgvector    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │   Ollama Local   │
                       │   (LLM + BGE-M3) │
                       └──────────────────┘
```

### Componentes Principales:

1. **Controllers**: Manejo de requests HTTP
2. **Services**: Lógica de negocio (RAG, Document, AI)
3. **Repositories**: Acceso a datos con JPA
4. **Entities**: Modelos de datos (Document, DocumentChunk, QAHistory)
5. **DTOs**: Objetos de transferencia de datos

## 📦 Instalación y Configuración

### Prerrequisitos
- Java 21+
- Maven 3.8+
- PostgreSQL 14+ con pgvector
- Ollama instalado localmente
- Docker (opcional, para PostgreSQL)

### 1. Configuración de la Base de Datos

**Opción A: Docker (Recomendado)**
```bash
docker-compose -f docker-compose-local.yml up -d
```

**Opción B: PostgreSQL local**
```sql
CREATE DATABASE legal_rag;
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Configuración de Ollama

```bash
# Instalar modelos necesarios
ollama pull llama3.2:latest
ollama pull bge-m3:latest
ollama pull deepseek-r1:latest  # Opcional
```

### 3. Configuración de la Aplicación

Editar `src/main/resources/application.properties`:

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/legal_rag
spring.datasource.username=postgres
spring.datasource.password=postgres

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:latest
spring.ai.ollama.embedding.options.model=bge-m3:latest
```

### 4. Ejecución

```bash
# Desarrollo
./mvnw spring-boot:run

# Producción
./mvnw clean package
java -jar target/rag-demo-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en: http://localhost:8080/swagger-ui.html

## 🎯 Uso del Sistema

### 1. Subir Documentos
```bash
curl -X POST "http://localhost:8080/api/documents/upload" \
  -F "file=@documento.pdf"
```

### 2. Hacer Preguntas
```bash
curl -X POST "http://localhost:8080/api/qa/ask" \
  -H "Content-Type: application/json" \
  -d '{"question": "¿Cuántos módulos tiene el programa?"}'
```

### 3. Cambiar Modelo LLM
```bash
curl -X POST "http://localhost:8080/api/models/change" \
  -H "Content-Type: application/json" \
  -d '{"modelName": "deepseek-r1:latest"}'
```

### 4. Ver Estado del Sistema
```bash
curl "http://localhost:8080/api/health"
```

## 🔮 Nuevas Características Propuestas

### 1. **Sistema de Anti-Alucinación Configurable por Sector**

**Problema actual**: El servicio anti-alucinación es genérico.

**Solución propuesta**: Servicios especializados por dominio/sector.

**Implementación sugerida:**

```java
// Interfaz base
public interface AntiHallucinationService {
    String createPrompt(String question, String context);
    boolean validateResponse(String response);
    String createFallbackResponse(String question);
}

// Implementaciones especializadas
@Component("legalAntiHallucination")
public class LegalAntiHallucinationService implements AntiHallucinationService {
    @Override
    public String createPrompt(String question, String context) {
        return """
            Como asistente legal especializado, analiza estos documentos jurídicos:
            
            DOCUMENTOS LEGALES:
            %s
            
            CONSULTA LEGAL: %s
            
            RESPUESTA LEGAL FUNDAMENTADA:
            """.formatted(context, question);
    }
}

@Component("medicalAntiHallucination")
public class MedicalAntiHallucinationService implements AntiHallucinationService {
    @Override
    public String createPrompt(String question, String context) {
        return """
            Como asistente médico, basándote en evidencia científica:
            
            LITERATURA MÉDICA:
            %s
            
            CONSULTA MÉDICA: %s
            
            RESPUESTA MÉDICA EVIDENCIADA:
            """.formatted(context, question);
    }
}

// Configuración dinámica
@Service
public class AntiHallucinationFactory {
    private final Map<String, AntiHallucinationService> services;
    
    public AntiHallucinationService getService(String sector) {
        return services.getOrDefault(sector + "AntiHallucination", 
                                   services.get("defaultAntiHallucination"));
    }
}
```

**Configuración por API:**
```java
@PostMapping("/api/config/sector")
public ResponseEntity<String> setSector(@RequestBody SectorRequest request) {
    configService.setSector(request.getSector());
    return ResponseEntity.ok("Sector configurado: " + request.getSector());
}
```

### 2. **Sistema de Prompts Personalizables**

**Implementación sugerida:**

```java
@Entity
public class PromptTemplate {
    @Id
    private String id;
    private String name;
    private String sector;
    private String template;
    private String description;
    private boolean active;
    private Map<String, String> variables;
}

@Service
public class PromptTemplateService {
    
    public List<PromptTemplate> getPromptsBySector(String sector) {
        return promptRepository.findBySectorAndActiveTrue(sector);
    }
    
    public String buildPrompt(String templateId, Map<String, String> variables) {
        PromptTemplate template = promptRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException("Template not found"));
        
        String prompt = template.getTemplate();
        for (Map.Entry<String, String> var : variables.entrySet()) {
            prompt = prompt.replace("{{" + var.getKey() + "}}", var.getValue());
        }
        return prompt;
    }
}
```

**Ejemplos de templates:**

```yaml
# Legal Sector
legal_consultation:
  template: |
    Como abogado especializado, analiza estos documentos legales:
    
    MARCO LEGAL:
    {{context}}
    
    CONSULTA: {{question}}
    
    Proporciona una respuesta fundamentada citando artículos específicos.
    
education_sector:
  template: |
    Como educador experto, basándote en este material pedagógico:
    
    MATERIAL EDUCATIVO:
    {{context}}
    
    PREGUNTA PEDAGÓGICA: {{question}}
    
    Explica de manera clara y didáctica.
```

**API para gestión de prompts:**
```java
@RestController
@RequestMapping("/api/prompts")
public class PromptController {
    
    @GetMapping("/sectors/{sector}")
    public List<PromptTemplate> getPromptsBySector(@PathVariable String sector) {
        return promptService.getPromptsBySector(sector);
    }
    
    @PostMapping
    public PromptTemplate createPrompt(@RequestBody PromptTemplate template) {
        return promptService.save(template);
    }
    
    @PutMapping("/{id}/activate")
    public void activatePrompt(@PathVariable String id) {
        promptService.activate(id);
    }
}
```

### 3. **Sistema de Configuración por Usuario/Organización**

```java
@Entity
public class OrganizationConfig {
    @Id
    private String organizationId;
    private String sector;
    private String activePromptTemplate;
    private String activeAntiHallucinationService;
    private Map<String, Object> customSettings;
    private double similarityThreshold;
    private int maxResults;
}

@Service
public class ConfigurationService {
    
    public void applyConfiguration(String organizationId, String userId) {
        OrganizationConfig config = getOrganizationConfig(organizationId);
        UserConfig userConfig = getUserConfig(userId);
        
        // Aplicar configuración específica
        setActivePromptService(config.getActivePromptTemplate());
        setAntiHallucinationService(config.getActiveAntiHallucinationService());
        setSearchParameters(config.getSimilarityThreshold(), config.getMaxResults());
    }
}
```

## 🚀 Mejoras Potenciales

### 1. **Optimización de Embeddings**
- **Caché de embeddings**: Redis para embeddings frecuentes
- **Embeddings jerárquicos**: Diferentes modelos por tipo de documento
- **Actualización incremental**: Re-embedding solo de chunks modificados

### 2. **Búsqueda Avanzada**
- **Filtros por metadatos**: Fecha, autor, tipo de documento
- **Búsqueda híbrida**: Combinación de semántica y keyword
- **Re-ranking**: Modelos especializados para mejorar resultados

### 3. **Gestión de Contexto Inteligente**
- **Resumen automático**: Condensación de contexto largo
- **Contexto conversacional**: Mantener historial de conversación
- **Contextualización dinámica**: Ajuste según tipo de pregunta

### 4. **Escalabilidad y Rendimiento**
- **Distribución**: Soporte para múltiples instancias
- **Cola de procesamiento**: Apache Kafka para documentos grandes
- **Caché distribuido**: Redis Cluster para alta disponibilidad

### 5. **Evaluación y Métricas**
- **Métricas de calidad**: BLEU, ROUGE para respuestas
- **Feedback de usuarios**: Sistema de rating de respuestas
- **A/B Testing**: Comparación entre diferentes configuraciones

### 6. **Seguridad y Governance**
- **Control de acceso**: RBAC para documentos sensibles
- **Auditoría**: Log de todas las consultas y respuestas
- **Cifrado**: Documentos y embeddings cifrados en reposo

### 7. **Interfaz de Usuario**
- **Dashboard web**: React/Vue.js para administración
- **Chat interface**: Interface conversacional
- **Analytics**: Métricas de uso y rendimiento

## 📚 API Endpoints

### Documentos
- `POST /api/documents/upload` - Subir documento
- `GET /api/documents` - Listar documentos
- `GET /api/documents/{id}` - Obtener documento específico
- `DELETE /api/documents/{id}` - Eliminar documento

### Preguntas y Respuestas
- `POST /api/qa/ask` - Hacer pregunta
- `POST /api/qa/ask/custom` - Pregunta con parámetros personalizados
- `GET /api/qa/history` - Historial de Q&A

### Gestión de Modelos
- `GET /api/models/available` - Modelos disponibles
- `GET /api/models/available/chat` - Solo modelos de chat
- `POST /api/models/change` - Cambiar modelo activo
- `GET /api/models/current` - Modelo actual

### Sistema
- `GET /api/health` - Estado del sistema
- `GET /api/stats` - Estadísticas del sistema

## 🤝 Contribuir al Proyecto

### Configuración del Entorno de Desarrollo

1. **Fork del repositorio**
2. **Configuración local:**
   ```bash
   git clone https://github.com/tu-usuario/rag-demo.git
   cd rag-demo
   cp application.properties.example application.properties
   # Editar configuración según tu entorno
   ```

3. **Ejecutar tests:**
   ```bash
   ./mvnw test
   ```

### Estructura del Proyecto

```
src/main/java/com/atuhome/ragdemo/
├── config/          # Configuraciones
├── controller/      # REST Controllers
├── exception/       # Excepciones personalizadas
├── model/          # Entidades y DTOs
├── repository/     # Repositories JPA
├── service/        # Lógica de negocio
│   ├── ai/         # Servicios de IA
│   ├── document/   # Gestión de documentos
│   ├── processing/ # Procesamiento de texto
│   └── rag/        # Core RAG services
└── util/           # Utilidades
```

### Guías de Contribución

1. **Crear feature branch:**
   ```bash
   git checkout -b feature/nueva-caracteristica
   ```

2. **Estándares de código:**
   - Java 21 features
   - Spring Boot best practices
   - Lombok para reducir boilerplate
   - Tests unitarios obligatorios

3. **Commit messages:**
   ```
   feat: agregar sistema de prompts personalizables
   fix: corregir búsqueda semántica con caracteres especiales
   docs: actualizar README con nuevos endpoints
   ```

4. **Pull Request:**
   - Descripción clara del cambio
   - Tests incluidos
   - Documentación actualizada

### Roadmap

- [ ] Sistema de prompts personalizables
- [ ] Anti-alucinación configurable por sector
- [ ] Dashboard web de administración
- [ ] Soporte para más tipos de documentos
- [ ] Integración con servicios cloud
- [ ] Métricas avanzadas y analytics
- [ ] API de feedback de usuarios

## 📄 Licencia



## 🆘 Soporte

Para preguntas, problemas o sugerencias:
- Crear un issue en GitHub
- Revisar la documentación en `/swagger-ui.html`
- Consultar los logs de la aplicación

---

**Nota**: Este proyecto es una demostración de un sistema RAG completo. Para uso en producción, considerar implementar todas las mejoras de seguridad y escalabilidad mencionadas.