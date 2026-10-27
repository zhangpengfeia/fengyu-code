#!/usr/bin/env bash
# Publish the education demo into the existing domain. Only the fengyucode project changes.
set -euo pipefail
umask 077

mode=${1:-}
base_dir=${2:-/project/fengyucode}
[[ "$mode" == deploy || "$mode" == rollback ]] || {
  echo 'Usage: pipeline-deploy.sh deploy BASE_DIR IMAGE_PREFIX IMAGE_TAG NETWORK HTTP_PORT | rollback BASE_DIR' >&2
  exit 2
}
[[ "$base_dir" =~ ^/[a-zA-Z0-9_/-]+$ && "$base_dir" != *..* && "$base_dir" != / ]] || exit 2
for tool in flock curl docker python3; do command -v "$tool" >/dev/null; done
docker compose version >/dev/null
ensure_agent_bridge() {
  local bridge=fengyucode-agent-bridge metadata
  if ! docker network inspect "$bridge" >/dev/null 2>&1; then
    # Another release may create it concurrently; validate the resulting network below.
    docker network create --driver bridge --internal "$bridge" >/dev/null 2>&1 || \
      docker network inspect "$bridge" >/dev/null
  fi
  metadata=$(docker network inspect --format '{{.Internal}} {{.Driver}}' "$bridge")
  [[ "$metadata" == 'true bridge' ]] || {
    echo '::error::fengyucode-agent-bridge must be an internal Docker bridge network' >&2
    exit 1
  }
}
ensure_agent_bridge
mkdir -p "$base_dir/.pipeline"
exec 9>"$base_dir/.pipeline/deploy.lock"
flock -w 600 9

compose() {
  local directory=$1
  shift
  (cd "$directory" && docker compose --project-name fengyucode "$@")
}

check_site() {
  local directory=$1 port expected response attempt
  port=$(sed -n 's/^HTTP_PORT=//p' "$directory/.env")
  expected=$(sed -n 's/^IMAGE_TAG=//p' "$directory/.env")
  [[ "$port" =~ ^[0-9]+$ ]] || { echo 'Invalid health-check HTTP_PORT' >&2; return 1; }
  echo "Checking container at 127.0.0.1:$port"
  if ! curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 "http://127.0.0.1:$port/healthz" >/dev/null; then
    echo 'Container HTTP health check failed' >&2
    return 1
  fi
  if ! response=$(curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 "http://127.0.0.1:$port/__release.txt"); then
    echo 'Could not read the container release marker' >&2
    return 1
  fi
  if [[ "$response" != "$expected" ]]; then
    printf 'Container release mismatch: expected=%s received=%.160s\n' "$expected" "$response" >&2
    return 1
  fi
  echo "Container version verified: $expected"
  if [[ -f "$directory/education.release" ]]; then
    if ! response=$(curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 "http://127.0.0.1:$port/prod-api/biz/auth/status"); then
      echo 'Education API readiness failed' >&2; return 1
    fi
    if ! printf '%s' "$response" | python3 -c 'import json,sys; data=json.load(sys.stdin); sys.exit(0 if data.get("status")=="UP" and data.get("release")==sys.argv[1] else 1)' "$expected"; then
      echo 'Education API version mismatch' >&2; return 1
    fi
    if ! response=$(curl -sS --noproxy '*' --connect-timeout 5 --max-time 10 -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/play/planet/index.html") || [[ "$response" != 403 ]]; then
      echo 'Anonymous planet access must return 403' >&2; return 1
    fi
  fi
  # Verify the real Caddy route, certificate, and image version as well as the container.
  for attempt in {1..12}; do
    echo "Checking Caddy HTTPS route ($attempt/12): fengyucode.zhangff.cn"
    if response=$(curl -fsS --noproxy '*' --connect-timeout 5 --max-time 10 \
      --resolve fengyucode.zhangff.cn:443:127.0.0.1 \
      https://fengyucode.zhangff.cn/__release.txt 2>&1); then
      if [[ "$response" == "$expected" ]]; then
        echo 'Caddy HTTPS route and release version verified'
        return 0
      fi
      printf 'Caddy release mismatch: expected=%s received=%.160s\n' "$expected" "$response" >&2
    else
      printf 'Caddy HTTPS request failed: %.300s\n' "$response" >&2
    fi
    if [[ "$attempt" != 12 ]]; then sleep 5; fi
  done
  echo 'Caddy HTTPS verification failed. Publish the fengyucode site block in the gateway project and check its certificate and Docker network.' >&2
  return 1
}

old_dir=''
if [[ -L "$base_dir/current" ]]; then
  old_dir=$(cd "$base_dir/current" && pwd -P)
  [[ -f "$old_dir/compose.yaml" && -f "$old_dir/.env" ]] || exit 1
elif [[ -e "$base_dir/current" ]]; then
  echo 'current must be a symlink, not a directory' >&2
  exit 1
fi
[[ ! -e "$base_dir/previous" || -L "$base_dir/previous" ]] || exit 1

if [[ "$mode" == deploy ]]; then
  prefix=${3:?IMAGE_PREFIX is required}
  tag=${4:?IMAGE_TAG is required}
  network=${5:-crm-ai_default}
  port=${6:-18088}
  [[ "$prefix" =~ ^ghcr\.io/[a-z0-9._/-]+$ ]] || exit 2
  [[ "$tag" =~ ^sha-[a-f0-9]{40}-[0-9]+-[0-9]+$ ]] || exit 2
  [[ "$network" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || exit 2
  [[ "$port" =~ ^[0-9]+$ ]] || exit 2
  (( 10#$port >= 1024 && 10#$port <= 65535 )) || exit 2
  new_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
  [[ "$new_dir" == "$base_dir/releases/$tag/deploy" ]] || {
    echo 'Extract the release into BASE_DIR/releases/IMAGE_TAG/deploy' >&2
    exit 2
  }
  [[ -f "$new_dir/education.release" ]] || { echo "Missing education release marker" >&2; exit 2; }
  [[ "$new_dir" != "$old_dir" ]] || { echo 'Release already active'; exit 0; }
  docker network inspect "$network" >/dev/null
  # The public test deployment uses the agreed admin login; keep other credentials stable.
  credentials="$base_dir/.pipeline/education.env"
  if [[ ! -f "$credentials" ]]; then
    python3 - "$credentials" <<'PYSECRETS'
import os,secrets,sys
fd=os.open(sys.argv[1],os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
with os.fdopen(fd,'w') as f:
    f.write('EDU_ADMIN_PASSWORD=admin123456\n')
    f.write('EDU_DEMO_PASSWORD=Edu!'+secrets.token_urlsafe(20)+'\n')
    f.write('EDU_TOKEN_SECRET='+secrets.token_hex(32)+'\n')
PYSECRETS
  fi
  [[ -f "$credentials" && ! -L "$credentials" ]] || exit 1
  chmod 600 "$credentials"
  # Update existing test deployments as well as fresh installations. Do not rotate
  # the demo-user password or token secret when standardizing the admin login.
  python3 - "$credentials" <<'PYADMIN'
from pathlib import Path
import sys
path = Path(sys.argv[1])
lines = path.read_text().splitlines()
lines = [line for line in lines if not line.startswith('EDU_ADMIN_PASSWORD=')]
lines.append('EDU_ADMIN_PASSWORD=admin123456')
path.write_text('\n'.join(lines) + '\n')
PYADMIN
  # Share only the integration credential with Java; never load Agent signing/DB keys.
  agent_base=${AGENT_DEPLOY_BASE_DIR:-/project/fengyucode-student-agent}
  [[ "$agent_base" =~ ^/[a-zA-Z0-9_/-]+$ && "$agent_base" != *..* && "$agent_base" != / ]] || exit 2
  agent_bridge_credentials="$base_dir/.pipeline/agent-bridge.env"
  python3 - "$agent_base/.pipeline/agent.env" "$agent_bridge_credentials" <<'PYBRIDGE'
import json
import os
from pathlib import Path
import sys
import tempfile
source, destination = map(Path, sys.argv[1:])
if source.is_symlink() or destination.is_symlink():
    raise SystemExit("Agent bridge credential files must not be symbolic links")
if source.exists():
    values = [line.partition("=")[2] for line in source.read_text().splitlines()
              if line.startswith("AGENT_SERVICE_API_KEY=")]
    if len(values) != 1:
        raise SystemExit("Agent integration key is missing or duplicated in server configuration")
    value = values[0]
    if value[:1] in ("'", '"') and value[-1:] == value[:1]:
        value = value[1:-1]
    if len(value) < 32 or any(character in value for character in "\r\n\x00"):
        raise SystemExit("Agent integration key must contain at least 32 single-line characters")
    # Compose double-quoted env values use dollar escaping; never interpolate secret content.
    contents = "EDU_AGENT_SERVICE_API_KEY=" + json.dumps(value.replace("$", "$$")) + "\n"
elif destination.exists():
    os.chmod(destination, 0o600)
    print("::notice::Agent source configuration is absent; keeping the existing bridge credential")
    sys.exit(0)
else:
    contents = "# Agent not deployed yet; the bridge endpoint will return a configuration error.\n"
    print("::notice::Agent configuration is absent; education remains available until Agent deployment")
fd, temporary = tempfile.mkstemp(prefix="agent-bridge-", dir=destination.parent)
try:
    os.fchmod(fd, 0o600)
    with os.fdopen(fd, "w") as stream:
        stream.write(contents)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, destination)
finally:
    if os.path.exists(temporary):
        os.unlink(temporary)
PYBRIDGE
  printf 'IMAGE_PREFIX=%s\nIMAGE_TAG=%s\nGATEWAY_NETWORK=%s\nHTTP_PORT=%s\n' \
    "$prefix" "$tag" "$network" "$port" > "$new_dir/.env"
  printf 'EDU_CREDENTIALS_FILE=%s\nEDU_AGENT_CREDENTIALS_FILE=%s\n' \
    "$credentials" "$agent_bridge_credentials" >> "$new_dir/.env"
  compose "$new_dir" config --quiet
  # Pull must succeed before changing any running container.
  echo "Pulling image: $prefix/web:$tag"
  compose "$new_dir" pull
else
  [[ -n "$old_dir" && -L "$base_dir/previous" ]] || {
    echo 'No previous release is recorded' >&2
    exit 1
  }
  new_dir=$(cd "$base_dir/previous" && pwd -P)
  [[ "$new_dir" != "$old_dir" && -f "$new_dir/compose.yaml" && -f "$new_dir/.env" ]] || exit 1
  compose "$new_dir" config --quiet
fi

switch_started=0
restore_on_failure() {
  local status=$?
  trap - EXIT
  if [[ "$status" != 0 && "$switch_started" == 1 ]]; then
    if [[ -n "$old_dir" ]]; then
      echo 'Release failed. Restoring the previous web image' >&2
      if compose "$old_dir" up -d --no-build --remove-orphans --wait --wait-timeout 150 && check_site "$old_dir"; then
        echo 'Previous release restored' >&2
      else
        echo 'Automatic recovery failed; inspect the fengyucode containers' >&2
        compose "$old_dir" ps -a || true
      fi
    else
      echo 'First release failed; removing the new web container' >&2
      compose "$new_dir" down || true
    fi
  fi
  exit "$status"
}
trap restore_on_failure EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

switch_started=1
compose "$new_dir" up -d --no-build --remove-orphans --wait --wait-timeout 150
check_site "$new_dir"
if [[ "$mode" == deploy ]]; then
  cp "${BASH_SOURCE[0]}" "$base_dir/.pipeline/rollback.sh.next"
  chmod 600 "$base_dir/.pipeline/rollback.sh.next"
  mv "$base_dir/.pipeline/rollback.sh.next" "$base_dir/.pipeline/rollback.sh"
fi
if [[ -n "$old_dir" ]]; then ln -sfn "$old_dir" "$base_dir/previous"; fi
ln -sfn "$new_dir" "$base_dir/current"
switch_started=0
compose "$new_dir" ps
echo "Application release active: $new_dir"
