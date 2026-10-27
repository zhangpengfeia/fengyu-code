#!/usr/bin/env sh
# Disposable single-process demo: no Redis and no MySQL.
set -eu
EDU_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
export EDU_ADMIN_PASSWORD="${EDU_ADMIN_PASSWORD:-admin123456}"
export EDU_DEMO_PASSWORD="${EDU_DEMO_PASSWORD:-DemoUser!2026}"
export EDU_TOKEN_SECRET="${EDU_TOKEN_SECRET:-$(openssl rand -hex 32)}"
printf '%s\n' 'Starting disposable demo (memory cache + H2). Data resets on backend restart.'
exec "$EDU_ROOT/scripts/run-dev.sh" "$@"
