#!/usr/bin/env sh
set -eu
EDU_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
PLANET_ROOT=$(CDPATH= cd -- "$EDU_ROOT/../gamesoptionproject" && pwd)
cd "$PLANET_ROOT"
[ -d node_modules ] || npm ci
GAME_BASE_PATH=/play/planet/ npm run build
mkdir -p "$EDU_ROOT/games/planet/v1"
cp -R dist/. "$EDU_ROOT/games/planet/v1/"
cp "$EDU_ROOT/games/planet/manifest.json" "$EDU_ROOT/games/planet/v1/manifest.json"
printf '%s\n' 'Planet v1 packaged behind /play/planet/ authentication.'
