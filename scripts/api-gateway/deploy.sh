#!/bin/bash
set -e

source ./calculate-hash.sh

CONTAINER_NAME="api-gateway:$CHECKSUM"

IMAGE_EXIST=$(eval $(minikube docker-env) && docker images --filter "reference=$CONTAINER_NAME" --quiet)

if [ -n "$IMAGE_EXIST" ]; then
    echo "Такой image уже есть"
else
    echo "Такого image еще нет"
    echo "Начинаем сборку образа"
    docker build -t $CONTAINER_NAME ../../back/services/api-gateway
    echo "Сборка окончена"

    echo "Закачиваем в minikube"
    minikube image load $CONTAINER_NAME
    echo "Закончили"
fi

export CONTAINER_NAME