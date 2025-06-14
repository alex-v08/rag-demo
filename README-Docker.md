# RAG Demo - Docker Deployment

Este proyecto está completamente dockerizado y listo para ejecutar con un solo comando.

## 🚀 Inicio Rápido

### Opción 1: Usar la imagen pre-construida (Recomendado)

```bash
# Descargar y ejecutar
curl -O https://raw.githubusercontent.com/atuhome/rag-demo/main/docker-compose.yml
docker-compose up -d
```

### Opción 2: Construir localmente

```bash
# Clonar el repositorio
git clone <repository-url>
cd rag-demo

# Ejecutar con el script
./run-docker.sh
```

## 📦 Servicios Incluidos

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| RAG Demo API | 8080 | Aplicación principal |
| PostgreSQL | 5432 | Base de datos con pgvector |
| Ollama | 11434 | Modelos de IA (llama3.2, bge-m3) |

## 🔧 URLs Importantes

- **API Principal**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Ollama API**: http://localhost:11434

## 📋 Verificación del Sistema

### 1. Verificar que todos los servicios estén funcionando:

```bash
docker-compose ps
```

### 2. Verificar la salud de la aplicación:

```bash
curl http://localhost:8080/actuator/health
```

### 3. Verificar que Ollama tiene los modelos:

```bash
curl http://localhost:11434/api/tags
```

## 🧪 Prueba del Sistema

### Cargar un documento PDF:

```bash
curl -X 'POST' \
  'http://localhost:8080/api/documents/upload' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@tu-documento.pdf'
```

### Hacer una pregunta:

```bash
curl -X 'POST' \
  'http://localhost:8080/api/qa/ask' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "¿Qué es una vista en SQL?"
  }'
```

## 🛠️ Comandos Útiles

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f rag-app

# Reiniciar la aplicación
docker-compose restart rag-app

# Parar todos los servicios
docker-compose down

# Parar y eliminar volúmenes (¡CUIDADO! Borra todos los datos)
docker-compose down -v

# Actualizar a la última versión
docker-compose pull
docker-compose up -d
```

## 🔄 Modelos de Ollama

El sistema descarga automáticamente:
- **llama3.2:latest** - Modelo de chat/generación
- **bge-m3:latest** - Modelo de embeddings

Esto puede tomar varios minutos en el primer inicio.

## 📊 Monitoreo

### Recursos del sistema:
```bash
docker stats
```

### Espacio usado por volúmenes:
```bash
docker system df
```

## 🚨 Troubleshooting

### Si la aplicación no inicia:

1. **Verificar puertos libres**:
   ```bash
   netstat -tulpn | grep -E ':(8080|5432|11434)'
   ```

2. **Verificar logs de errores**:
   ```bash
   docker-compose logs rag-app
   ```

3. **Reiniciar servicios**:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

### Si Ollama no funciona:

1. **Verificar descarga de modelos**:
   ```bash
   docker-compose logs ollama-init
   ```

2. **Descargar modelos manualmente**:
   ```bash
   docker exec rag-ollama ollama pull llama3.2:latest
   docker exec rag-ollama ollama pull bge-m3:latest
   ```

## ⚙️ Configuración Avanzada

### Variables de entorno disponibles:

- `SPRING_DATASOURCE_URL`: URL de la base de datos
- `SPRING_AI_OLLAMA_BASE_URL`: URL de Ollama
- `SPRING_PROFILES_ACTIVE`: Perfil activo (docker por defecto)

### Personalizar configuración:

Crear un archivo `.env` en el directorio del proyecto:

```env
POSTGRES_PASSWORD=mi_password_seguro
OLLAMA_MODELS=llama3.2:latest,bge-m3:latest
RAG_UPLOAD_MAX_SIZE=100MB
```

## 🏗️ Para Desarrolladores

### Construir imagen local:

```bash
docker build -t atuhome/rag-demo:v0.01 .
```

### Ejecutar solo la aplicación (con servicios externos):

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/legal_rag \
  -e SPRING_AI_OLLAMA_BASE_URL=http://host:11434 \
  atuhome/rag-demo:v0.01
```

## 📝 Notas

- Los datos se persisten en volúmenes Docker
- La aplicación usa el perfil `docker` automáticamente
- Todos los servicios están en la misma red Docker
- Health checks aseguran orden de inicio correcto