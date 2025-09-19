#!/bin/bash
set -e

# Цвета для логов
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
NC='\033[0m'

echo -e "${BLUE}[api-gateway]${NC} Запуск скрипта развертывания."

source ../calculate-hash.sh
source ../../env/deploy-auth-service.env
source ../../env/deploy-universal.env

# Генерация и проверка хэша
echo -e "${BLUE}[api-gateway]${NC} Вычисляем хэш для образа..."
generate_checksum "${HASH_PATH}"

IMAGE_NAME="${IMAGE_PREFIX}-${CHECKSUM_GLOBAL}"
echo -e "${YELLOW}[api-gateway]${NC} Имя образа: ${IMAGE_NAME}"

eval $(minikube docker-env)

# Проверка и сборка образа
echo -e "${BLUE}[api-gateway]${NC} Проверяем наличие образа в Minikube..."
if docker images | grep -q "${IMAGE_NAME}"; then
  echo -e "${GREEN}[api-gateway]${NC} Образ '${IMAGE_NAME}' уже существует в Minikube. Пропускаем сборку."
else
  echo -e "${BLUE}[api-gateway]${NC} 📦 Образ '${IMAGE_NAME}' не найден. Начинаем сборку..."
  docker build -t "${IMAGE_NAME}" $DOCKER_PATH
  echo -e "${GREEN}[api-gateway]${NC} Образ '${IMAGE_NAME}' успешно собран."
  docker tag "${IMAGE_NAME}" "${IMAGE_PREFIX}:latest"
  echo -e "${GREEN}[api-gateway]${NC} Образ '${IMAGE_NAME}' успешно собран с дополнительным тегом '${IMAGE_PREFIX}-latest'."
fi


echo -e "${BLUE}[api-gateway]${NC} Проверяем существование Deployment..."
if kubectl get deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE} &>/dev/null; then
  echo -e "${YELLOW}[api-gateway]${NC} Deployment '${DEPLOYMENT_NAME}' уже существует. Обновляем образ..."
  kubectl set image deployment/${DEPLOYMENT_NAME} ${DEPLOYMENT_NAME}=${IMAGE_NAME} -n ${NAMESPACE}
  echo -e "${GREEN}[api-gateway]${NC} Образ в Deployment успешно обновлен."
else
  echo -e "${BLUE}[api-gateway]${NC} Deployment '${DEPLOYMENT_NAME}' не найден. Создаем новый..."
  IMAGE_NAME="${IMAGE_NAME}" envsubst < "${DEPLOYMENT_TEMPLATE_PATH}" | kubectl apply -n ${NAMESPACE} -f -
  echo -e "${GREEN}[api-gateway]${NC} Deployment '${DEPLOYMENT_NAME}' успешно создан."
fi


echo -e "${BLUE}[api-gateway]${NC} Проверяем существование Service..."
if kubectl get service ${SERVICE_NAME} -n ${NAMESPACE} &>/dev/null; then
  echo -e "${YELLOW}[api-gateway]${NC} Service '${SERVICE_NAME}' уже существует. Пропускаем создание."
else
  echo -e "${BLUE}[api-gateway]${NC} Service '${SERVICE_NAME}' не найден. Создаем новый..."
  kubectl apply -n ${NAMESPACE} -f "${SERVICE_TEMPLATE_PATH}"
  echo -e "${GREEN}[api-gateway]${NC} Service '${SERVICE_NAME}' успешно создан."
fi

echo -e "${GREEN}[api-gateway]${NC} Скрипт развертывания завершен успешно."