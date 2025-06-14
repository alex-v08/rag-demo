#!/bin/bash

# Script para ejecutar RAG Demo con Ollama local
set -e

echo "=== Iniciando RAG Demo con Ollama local ==="

# Verificar si Docker está ejecutándose
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker no está ejecutándose. Por favor inicia Docker y vuelve a intentar."
    exit 1
fi

# Verificar si Ollama está ejecutándose localmente
echo "Verificando Ollama local..."
if ! curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "❌ Error: Ollama no está ejecutándose en tu máquina local."
    echo ""
    echo "Por favor, asegúrate de:"
    echo "1. Tener Ollama instalado (https://ollama.com)"
    echo "2. Ejecutar 'ollama serve' en otra terminal"
    echo "3. Descargar los modelos requeridos:"
    echo "   - ollama pull llama3.2:latest"
    echo "   - ollama pull bge-m3:latest"
    echo ""
    exit 1
fi

# Verificar modelos instalados
echo "Verificando modelos de Ollama..."
MODELS=$(curl -s http://localhost:11434/api/tags | grep -E "(llama3.2|bge-m3)" || true)

if [[ -z "$MODELS" ]]; then
    echo "⚠️  Advertencia: No se encontraron los modelos requeridos."
    echo "Descargando modelos..."
    echo ""
    echo "Descargando llama3.2:latest..."
    ollama pull llama3.2:latest
    echo ""
    echo "Descargando bge-m3:latest..."
    ollama pull bge-m3:latest
    echo ""
fi

# Crear directorio de uploads si no existe
mkdir -p uploads

# Detener contenedores existentes
echo "Deteniendo contenedores existentes..."
docker-compose -f docker-compose.local.yml down

# Descargar la imagen más reciente
echo "Descargando imagen de Docker Hub..."
docker pull atuhome/rag-demo:v0.01

# Iniciar servicios
echo "Iniciando servicios..."
docker-compose -f docker-compose.local.yml up -d

# Esperar a que los servicios estén listos
echo "Esperando a que los servicios estén listos..."
sleep 10

# Verificar estado de los servicios
echo ""
echo "=== Verificando servicios ==="

# PostgreSQL
if docker-compose -f docker-compose.local.yml exec postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "✅ PostgreSQL: Funcionando"
else
    echo "❌ PostgreSQL: No está listo"
fi

# Aplicación
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ RAG Demo App: Funcionando"
else
    echo "⏳ RAG Demo App: Iniciando... (puede tomar hasta 1 minuto)"
fi

# Ollama local
if curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "✅ Ollama: Funcionando (local)"
else
    echo "❌ Ollama: No disponible"
fi

echo ""
echo "=== Servicios disponibles ==="
echo ""
echo "📱 Aplicación RAG Demo:"
echo "   URL: http://localhost:8080"
echo "   Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   Health: http://localhost:8080/actuator/health"
echo ""
echo "🗄️  PostgreSQL:"
echo "   Host: localhost"
echo "   Puerto: 5432"
echo "   Base de datos: legal_rag"
echo "   Usuario: postgres"
echo "   Password: postgres"
echo ""
echo "🤖 Ollama (local):"
echo "   URL: http://localhost:11434"
echo ""
echo "📊 PgAdmin (opcional):"
echo "   Ejecutar: docker-compose -f docker-compose.local.yml --profile tools up -d"
echo "   URL: http://localhost:8081"
echo "   Email: admin@example.com"
echo "   Password: admin"
echo ""
echo "=== Comandos útiles ==="
echo ""
echo "Ver logs:"
echo "  docker-compose -f docker-compose.local.yml logs -f"
echo ""
echo "Detener servicios:"
echo "  docker-compose -f docker-compose.local.yml down"
echo ""
echo "Reiniciar aplicación:"
echo "  docker-compose -f docker-compose.local.yml restart rag-app"
echo ""