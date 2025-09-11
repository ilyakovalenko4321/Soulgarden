#!/bin/bash
set -e
echo "Началась сборка хэша api-gateway"


SRC_DIR="../../back/services/api-gateway"

CHECKSUM=$(find "$SRC_DIR" -type f \( -name '*.java' -o -name '*.xml' \) \
  -not -path '*/.git/*' -not -path '*/target/*' -not -path '*/build/*' \
  -print0 \
  | sort -z \
  | xargs -0 sha1sum \
  | sha1sum \
  | awk '{print $1}' \
  | cut -c 1-16)

echo "Сборка хэша закончена. checksum=$CHECKSUM"
export CHECKSUM
