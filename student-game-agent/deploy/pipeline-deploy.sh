#!/usr/bin/env bash
# Deploy only the independent student/Agent stack; never delete database volumes.
set -euo pipefail
umask 077
mode=${1:-}
base=${2:-/project/fengyucode-student-agent}
[[ "$mode" == deploy || "$mode" == rollback ]] || exit 2
[[ "$base" =~ ^/[a-zA-Z0-9_/-]+$ && "$base" != *..* && "$base" != / ]] || exit 2
for command in docker flock curl python3; do command -v "$command" >/dev/null; done
docker compose version >/dev/null
mkdir -p "$base/.pipeline"
exec 9>"$base/.pipeline/deploy.lock"
flock -w 600 9
credentials="$base/.pipeline/agent.env"
[[ -s "$credentials" && ! -L "$credentials" ]] || {
  echo 'Prepare the server-only .pipeline/agent.env from release.env.example before deployment.' >&2
  exit 1
}
chmod 600 "$credentials"
# Hex database passwords do not need URL escaping in the SQLAlchemy DSN.
postgres_password=$(sed -n 's/^POSTGRES_PASSWORD=//p' "$credentials")
[[ "$postgres_password" =~ ^[a-fA-F0-9]{64,}$ ]] || { echo 'POSTGRES_PASSWORD must be a separately generated hex secret (32+ bytes).' >&2; exit 1; }
unset postgres_password
compose() {
  local directory=$1
  shift
  docker compose --project-name fengyucode-student-agent --env-file "$credentials" \
    --env-file "$directory/.env" -f "$directory/compose.yaml" "$@"
}
check_site() {
  local directory=$1 port tag actual code
  port=$(sed -n 's/^HTTP_PORT=//p' "$directory/.env")
  tag=$(sed -n 's/^IMAGE_TAG=//p' "$directory/.env")
  curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 "http://127.0.0.1:$port/healthz" >/dev/null
  actual=$(curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 "http://127.0.0.1:$port/education/agent/__release.txt")
  [[ "$actual" == "$tag" ]] || return 1
  for private in dev/mock/widget-session internal/v1/tokens; do
    code=$(curl -sS --noproxy '*' --max-time 10 -o /dev/null -w '%{http_code}' -X POST "http://127.0.0.1:$port/education/agent/api/$private")
    [[ "$code" == 404 ]] || return 1
  done
  # This also verifies that the existing education gateway routes the new prefix.
  actual=$(curl -fsS --noproxy '*' --connect-timeout 5 --max-time 15 \
    --resolve fengyucode.zhangff.cn:443:127.0.0.1 \
    https://fengyucode.zhangff.cn/education/agent/__release.txt)
  [[ "$actual" == "$tag" ]]
}
link_release() {
  python3 - "$1" "$2" <<'PYTHON'
import os
import sys
source, destination = sys.argv[1:]
temporary = destination + ".new"
if os.path.lexists(temporary):
    os.unlink(temporary)
os.symlink(source, temporary)
os.replace(temporary, destination)
PYTHON
}
old=''
if [[ -L "$base/current" ]]; then old=$(cd "$base/current" && pwd -P)
elif [[ -e "$base/current" ]]; then echo 'current must be a release symlink' >&2; exit 1; fi
[[ ! -e "$base/previous" || -L "$base/previous" ]] || exit 1
[[ -z "$old" || "$old" == "$base"/releases/*/deploy ]] || exit 1
if [[ "$mode" == deploy ]]; then
  prefix=${3:?IMAGE_PREFIX required}; tag=${4:?IMAGE_TAG required}; network=${5:-crm-ai_default}; port=${6:-18089}
  [[ "$prefix" =~ ^ghcr\.io/[a-z0-9._/-]+$ && "$tag" =~ ^sha-[a-f0-9]{40}-[0-9]+-[0-9]+$ ]] || exit 2
  [[ "$network" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ && "$port" =~ ^[0-9]+$ ]] || exit 2
  (( 10#$port >= 1024 && 10#$port <= 65535 )) || exit 2
  directory=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
  [[ "$directory" == "$base/releases/$tag/deploy" ]] || exit 2
  docker network inspect "$network" >/dev/null
  printf 'IMAGE_PREFIX=%s\nIMAGE_TAG=%s\nGATEWAY_NETWORK=%s\nHTTP_PORT=%s\nAGENT_CREDENTIALS_FILE=%s\n' \
    "$prefix" "$tag" "$network" "$port" "$credentials" > "$directory/.env"
else
  [[ -L "$base/previous" ]] || { echo 'No previous Agent release is available' >&2; exit 1; }
  directory=$(cd "$base/previous" && pwd -P)
  [[ "$directory" == "$base"/releases/*/deploy ]] || exit 1
fi
[[ -f "$directory/compose.yaml" && -f "$directory/.env" ]] || exit 1
compose "$directory" config --quiet
# Pull and migrations complete before the API/web containers switch.
compose "$directory" --profile tools pull
changed=0
recover() {
  local code=$?
  if (( code != 0 && changed )); then
    if [[ -n "$old" && "$old" != "$directory" ]]; then
      echo 'Restoring the previous student/Agent images after a failed release.' >&2
      compose "$old" up -d --wait --wait-timeout 180 agent-api worker student-web || true
    elif [[ -z "$old" ]]; then
      compose "$directory" stop agent-api worker student-web || true
    fi
  fi
  exit "$code"
}
trap recover EXIT
changed=1
compose "$directory" up -d --wait --wait-timeout 180 postgres
compose "$directory" run --rm --no-deps migrate
compose "$directory" up -d --wait --wait-timeout 180 agent-api worker student-web
check_site "$directory" || { echo 'Agent public-path verification failed; deploy the education gateway prefix first.' >&2; exit 1; }
if [[ -n "$old" && "$old" != "$directory" ]]; then
  link_release "$old" "$base/previous"
fi
link_release "$directory" "$base/current"
trap - EXIT
printf 'Student/Agent release verified at https://fengyucode.zhangff.cn/education/agent/\n'
