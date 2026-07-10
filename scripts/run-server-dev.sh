#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$REPO_ROOT/server/.env"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
ANDROID_HELPER="$SCRIPT_DIR/configure-android-wsl-portproxy.ps1"
ANDROID_DEVICE=false

usage() {
  printf 'Usage: %s [--android-device]\n' "${0##*/}" >&2
}

case "$#" in
  0)
    ;;
  1)
    if [[ "$1" != "--android-device" ]]; then
      usage
      exit 2
    fi
    ANDROID_DEVICE=true
    ;;
  *)
    usage
    exit 2
    ;;
esac

if [[ ! -f "$ENV_FILE" ]]; then
  printf 'Error: missing environment file: %s\n' "$ENV_FILE" >&2
  printf 'Create server/.env before running the backend.\n' >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  printf 'Error: missing Docker Compose file: %s\n' "$COMPOSE_FILE" >&2
  exit 1
fi

load_env_file() {
  local line
  local key
  local value

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"

    if [[ -z "$line" || "$line" == \#* ]]; then
      continue
    fi

    if [[ "$line" != *=* ]]; then
      printf 'Error: invalid line in %s: %s\n' "$ENV_FILE" "$line" >&2
      exit 1
    fi

    key="${line%%=*}"
    value="${line#*=}"

    if [[ ! "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      printf 'Error: invalid variable name in %s: %s\n' "$ENV_FILE" "$key" >&2
      exit 1
    fi

    export "$key=$value"
  done < "$ENV_FILE"
}

detect_compose_command() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
    return 0
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
    return 0
  fi

  printf 'Error: neither docker compose nor docker-compose is available.\n' >&2
  printf 'Install Docker Compose to start the local PostgreSQL service.\n' >&2
  exit 1
}

start_postgres_service() {
  printf 'Starting local PostgreSQL via Docker Compose...\n'
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up -d postgres
}

wait_for_postgres() {
  local container_id
  local status
  local attempt
  local max_attempts=15

  container_id="$("${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" ps -q postgres)"

  if [[ -z "$container_id" ]]; then
    printf 'Error: could not resolve the PostgreSQL container ID from Docker Compose.\n' >&2
    exit 1
  fi

  printf 'Waiting for PostgreSQL to become ready...\n'

  for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)"

    case "$status" in
      healthy|running)
        printf 'PostgreSQL is ready.\n'
        return 0
        ;;
      exited|dead)
        printf 'Error: PostgreSQL container stopped before becoming ready.\n' >&2
        exit 1
        ;;
    esac

    sleep 2
  done

  printf 'Error: PostgreSQL did not become ready in time.\n' >&2
  printf 'Check container logs with: %s -f %q logs postgres\n' "${COMPOSE_CMD[*]}" "$COMPOSE_FILE" >&2
  exit 1
}

configure_android_device() {
  local distro_name

  if [[ ! -f "$ANDROID_HELPER" ]]; then
    printf 'Error: missing Android connectivity helper: %s\n' "$ANDROID_HELPER" >&2
    exit 1
  fi

  if ! command -v powershell.exe >/dev/null 2>&1; then
    printf 'Error: --android-device requires Windows PowerShell through WSL interop.\n' >&2
    exit 1
  fi

  distro_name="${WSL_DISTRO_NAME:-FedoraLinux-43}"
  if [[ ! "$distro_name" =~ ^[A-Za-z0-9._-]+$ ]]; then
    printf 'Error: unsupported WSL distribution name: %s\n' "$distro_name" >&2
    exit 1
  fi

  printf 'Preparing Android USB connectivity through Windows...\n'
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$ANDROID_HELPER" -DistroName "$distro_name"
}

load_env_file
detect_compose_command
start_postgres_service
wait_for_postgres

if [[ "$ANDROID_DEVICE" == true ]]; then
  configure_android_device
fi

exec "$REPO_ROOT/gradlew" :server:run
