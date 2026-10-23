#!/usr/bin/env bash
# Run on the Linux production host. Only the fengyucode compose project is changed.
set -euo pipefail
umask 077

mode=${1:-}
base_dir=${2:-/project/fengyucode}
[[ "$mode" == deploy || "$mode" == rollback ]] || {
  echo 'Usage: pipeline-deploy.sh deploy BASE_DIR IMAGE_PREFIX IMAGE_TAG NETWORK HTTP_PORT | rollback BASE_DIR' >&2
  exit 2
}
[[ "$base_dir" =~ ^/[a-zA-Z0-9_/-]+$ && "$base_dir" != *..* && "$base_dir" != / ]] || exit 2
for tool in flock curl docker; do command -v "$tool" >/dev/null; done
docker compose version >/dev/null
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
  [[ "$new_dir" != "$old_dir" ]] || { echo 'Release already active'; exit 0; }
  docker network inspect "$network" >/dev/null
  printf 'IMAGE_PREFIX=%s\nIMAGE_TAG=%s\nGATEWAY_NETWORK=%s\nHTTP_PORT=%s\n' \
    "$prefix" "$tag" "$network" "$port" > "$new_dir/.env"
  compose "$new_dir" config --quiet
  # Pull must succeed before changing any running container.
  echo "Pulling image: $prefix/web:$tag"
  compose "$new_dir" pull fengyucode-web
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
      if compose "$old_dir" up -d --no-build --wait --wait-timeout 90 && check_site "$old_dir"; then
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
compose "$new_dir" up -d --no-build --wait --wait-timeout 90
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
