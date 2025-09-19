#!/bin/bash

set -e

source ../../../env/deploy-redis.env

kubectl apply -k "${KUSTOMIZATION_PATH}"
