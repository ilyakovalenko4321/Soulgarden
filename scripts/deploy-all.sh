#!/bin/bash
set -e

NAMESPACE="soulgarden"

echo "Шаг 1: Запускаем сборку образов"
./docker-build-all.sh

echo "Шаг 2: Применяем Kubernetes манифесты"
kubectl apply -k ../k8s/

sleep 10
kubectl get pods -n $NAMESPACE

kubectl get svc -n $NAMESPACE

echo "Сборка окончено"