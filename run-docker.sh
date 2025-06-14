#!/bin/bash

# Script to run the RAG Demo using Docker Compose
set -e

echo "=== Starting RAG Demo with Docker Compose ==="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Create necessary directories
echo "Creating necessary directories..."
mkdir -p uploads

# Pull the latest images (optional)
echo "Pulling latest images..."
docker-compose pull

# Start the services
echo "Starting services..."
docker-compose up -d

echo "=== Services Starting ==="
echo "PostgreSQL: http://localhost:5432"
echo "Ollama: http://localhost:11434"
echo "RAG Demo API: http://localhost:8080"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo ""

# Wait for services to be ready
echo "Waiting for services to be ready..."
echo "This may take a few minutes as Ollama downloads the required models..."

# Monitor logs
echo "=== Checking service status ==="
docker-compose ps

echo ""
echo "To view logs: docker-compose logs -f"
echo "To stop services: docker-compose down"
echo ""
echo "The application will be ready when all health checks pass."
echo "You can check health status with: curl http://localhost:8080/actuator/health"