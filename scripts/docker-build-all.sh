#!/bin/bash
set -e

# НЕ используем minikube docker-env, чтобы строить локально

echo "👉 Собираем образы наших сервисов локально..."

LOCAL_VERSION=$(date +%Y%m%d%H%M)

API_GATEWAY_NAME="api-gateway-service"
API_GATEWAY_TAG="$API_GATEWAY_NAME:$LOCAL_VERSION"
API_GATEWAY_LATEST="$API_GATEWAY_NAME:latest"

echo "👉 Собираем api-gateway локально..."
docker build -t $API_GATEWAY_TAG ../back/services/api-gateway
docker tag $API_GATEWAY_TAG $API_GATEWAY_LATEST

echo "✅ Локальные сервисы собраны."

echo "👉 Загружаем образ в Minikube..."
minikube image load $API_GATEWAY_LATEST
minikube image load $API_GATEWAY_TAG  # Если нужно загрузить и версионный тег

echo "✅ Образ загружен в Minikube."