#!/usr/bin/env sh
set -eu
EDU_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
: "${EDU_ADMIN_PASSWORD:?Set EDU_ADMIN_PASSWORD (unique initial admin password)}"
: "${EDU_TOKEN_SECRET:?Set EDU_TOKEN_SECRET (at least 32 random characters)}"
export SPRING_PROFILES_ACTIVE=dev
export EDU_GAMES_ROOT="$EDU_ROOT/games"
cd "$EDU_ROOT/backend"
./mvnw -B -ntp -DskipTests package
exec java -jar ruoyi-admin/target/ruoyi-admin.jar "$@"
