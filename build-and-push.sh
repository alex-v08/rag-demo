#!/bin/bash

# Build and push script for RAG Demo
set -e

IMAGE_NAME="atuhome/rag-demo"
TAG="v0.01"
FULL_IMAGE="${IMAGE_NAME}:${TAG}"

echo "=== Building RAG Demo Docker Image ==="

# Build the Docker image
echo "Building image: $FULL_IMAGE"
docker build -t $FULL_IMAGE .

# Tag as latest
echo "Tagging as latest..."
docker tag $FULL_IMAGE ${IMAGE_NAME}:latest

echo "=== Pushing to Docker Hub ==="

# Login to Docker Hub (prompt for credentials if needed)
echo "Please ensure you're logged in to Docker Hub..."
docker login

# Push both tags
echo "Pushing $FULL_IMAGE..."
docker push $FULL_IMAGE

echo "Pushing ${IMAGE_NAME}:latest..."
docker push ${IMAGE_NAME}:latest

echo "=== Build and Push Complete ==="
echo "Image available at: $FULL_IMAGE"
echo "Latest available at: ${IMAGE_NAME}:latest"