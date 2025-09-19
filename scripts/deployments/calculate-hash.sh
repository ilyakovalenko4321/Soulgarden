#!/bin/bash
set -e

generate_checksum() {
  echo -e ""
  local SRC_DIR="$1"
  local RED='\033[0;31m'
  local GREEN='\033[0;32m'
  local YELLOW='\033[1;33m'
  local NC='\033[0m'

  echo -e "${NC}[CHECKSUM-GEN]${NC} Начинаем генерацию контрольной суммы для директории: ${SRC_DIR}"

  local CHECKSUM
  CHECKSUM=$(find "$SRC_DIR" -type f \( -name '*.java' -o -name '*.xml' \) \
    -not -path '*/.git/*' -not -path '*/target/*' -not -path '*/build/*' -not -path '*/.idea/*' -not -path '*/.mvn/*' \
    -print0 \
    | sort -z \
    | xargs -0 sha1sum \
    | sha1sum \
    | awk '{print $1}' \
    | cut -c 1-16)

  if [ -z "$CHECKSUM" ]; then
    echo -e "${RED}[CHECKSUM-GEN]${NC} Ошибка: Не удалось сгенерировать контрольную сумму. Проверьте путь или наличие файлов."
    exit 1
  fi

  echo -e "${GREEN}[CHECKSUM-GEN]${NC} Генерация контрольной суммы завершена успешно."
  echo -e "${GREEN}[CHECKSUM-GEN]${NC} Рассчитанная контрольная сумма: ${CHECKSUM}\n"

  export CHECKSUM_GLOBAL="${CHECKSUM}"
}