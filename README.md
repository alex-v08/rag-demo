# RAG Demo - Sistema de Retrieval-Augmented Generation

Sistema de preguntas y respuestas basado en documentos PDF usando RAG (Retrieval-Augmented Generation) con Spring Boot, PostgreSQL con pgvector y Ollama.

## 🚀 Características

- **Carga de documentos PDF**: Extracción automática de texto y procesamiento
- **Búsqueda semántica**: Usando embeddings y similitud vectorial
- **Respuestas contextualizadas**: Generación de respuestas basadas en el contenido de los documentos
- **API REST**: Endpoints completos con documentación Swagger
- **Persistencia**: PostgreSQL con extensión pgvector para almacenamiento de embeddings

## 📋 Pre-requisitos

### Opción 1: Ejecución Local (Recomendado para desarrollo)

- **Java 21** o superior
- **PostgreSQL 16** con extensión pgvector
- **Maven 3.8+**
- **Ollama** instalado localmente con los modelos requeridos

### Opción 2: Ejecución con Docker

- **Docker** y **Docker Compose**
- Los modelos de Ollama se descargan automáticamente

## 🛠️ Instalación de Ollama (Ejecución Local)

### 1. Instalar Ollama

**Linux/WSL:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**macOS:**
```bash
brew install ollama
```

**Windows:**
Descargar desde [ollama.com](https://ollama.com/download)

### 2. Iniciar el servicio Ollama

```bash
ollama serve
```

### 3. Descargar los modelos requeridos

```bash
# Modelo de chat/generación
ollama pull llama3.2:latest

# Modelo de embeddings
ollama pull bge-m3:latest
```

### 4. Verificar instalación

```bash
# Listar modelos instalados
ollama list

# Verificar que el servicio esté funcionando
curl http://localhost:11434/api/version
```

## 🚀 Ejecución

### Opción 1: Ejecución Local

#### 1. Configurar PostgreSQL con pgvector

```sql
-- Crear base de datos
CREATE DATABASE legal_rag;

-- Conectarse a la base de datos
\c legal_rag;

-- Instalar extensión pgvector
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 2. Configurar application.properties

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/legal_rag
spring.datasource.username=postgres
spring.datasource.password=tu_password

# Ollama (debe estar ejecutándose localmente)
spring.ai.ollama.base-url=http://localhost:11434
```

#### 3. Ejecutar la aplicación

```bash
# Compilar y ejecutar
./mvnw spring-boot:run

# O construir JAR y ejecutar
./mvnw clean package
java -jar target/rag-demo-0.0.1-SNAPSHOT.jar
```

### Opción 2: Ejecución con Docker (Más simple)

```bash
# Descargar docker-compose.yml
curl -O https://raw.githubusercontent.com/atuhome/rag-demo/main/docker-compose.yml

# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f
```

**Nota**: Docker descargará automáticamente los modelos de Ollama, esto puede tomar varios minutos en el primer inicio.

## 📝 Uso del Sistema

### 1. Verificar que el sistema esté funcionando

```bash
curl http://localhost:8080/actuator/health
```

### 2. Cargar un documento PDF

```bash
curl -X 'POST' \
  'http://localhost:8080/api/documents/upload' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@tu-documento.pdf'
```

### 3. Hacer preguntas sobre el documento

```bash
curl -X 'POST' \
  'http://localhost:8080/api/qa/ask' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "¿Cuál es el tema principal del documento?"
  }'
```

### 4. Interfaz Swagger UI

Acceder a: http://localhost:8080/swagger-ui.html

## 🔧 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/documents/upload` | Cargar un documento PDF |
| GET | `/api/documents` | Listar todos los documentos |
| DELETE | `/api/documents/{id}` | Eliminar un documento |
| POST | `/api/qa/ask` | Hacer una pregunta |
| GET | `/api/qa/history` | Ver historial de preguntas |

## ⚙️ Configuración

### Variables de entorno importantes

```bash
# Base de datos
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/legal_rag
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Ollama
SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434

# Modelos
SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL=llama3.2:latest
SPRING_AI_OLLAMA_EMBEDDING_OPTIONS_MODEL=bge-m3:latest
```

### Parámetros de configuración RAG

```properties
# Tamaño de chunks
app.rag.chunk.size=1000
app.rag.chunk.overlap=200

# Búsqueda semántica
app.rag.search.similarity-threshold=0.2
app.rag.search.max-results=5

# Dimensión de embeddings
app.rag.embedding.dimension=1024
```

## 🐳 Docker

### Imagen disponible en Docker Hub

```bash
docker pull atuhome/rag-demo:v0.01
```

### Docker Compose completo

El proyecto incluye un `docker-compose.yml` que levanta:
- PostgreSQL con pgvector
- Ollama con modelos pre-configurados
- La aplicación RAG Demo

## 🧪 Pruebas

### Documento de ejemplo

Crear un PDF de prueba:

```bash
echo "¿Qué es una vista SQL?
Una vista es una tabla virtual basada en el resultado de una consulta SQL." > test.txt

# Convertir a PDF (requiere LibreOffice)
libreoffice --headless --convert-to pdf test.txt
```

### Flujo de prueba completo

```bash
# 1. Cargar documento
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.pdf"

# 2. Hacer pregunta
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "¿Qué es una vista SQL?"}'
```

## 🚨 Solución de Problemas

### Ollama no responde

```bash
# Verificar que Ollama esté ejecutándose
ps aux | grep ollama

# Reiniciar servicio
ollama serve

# Verificar modelos instalados
ollama list
```

### Error de conexión a PostgreSQL

```bash
# Verificar que PostgreSQL esté ejecutándose
sudo systemctl status postgresql

# Verificar extensión pgvector
psql -U postgres -d legal_rag -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

### Modelos no encontrados

```bash
# Descargar modelos manualmente
ollama pull llama3.2:latest
ollama pull bge-m3:latest
```

## 📚 Tecnologías Utilizadas

- **Spring Boot 3.5.0**: Framework principal
- **Spring AI**: Integración con Ollama
- **PostgreSQL + pgvector**: Base de datos con soporte vectorial
- **Ollama**: Modelos de IA locales
- **Apache PDFBox**: Extracción de texto de PDFs
- **Docker**: Contenerización

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

