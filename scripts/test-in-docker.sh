#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME=${IMAGE_NAME:-transloadit-java-sdk-dev}
CACHE_DIR=.gradle-docker

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required to run this script." >&2
    exit 1
  fi

  if ! docker info >/dev/null 2>&1; then
    if [[ -z "${DOCKER_HOST:-}" && -S "$HOME/.colima/default/docker.sock" ]]; then
      export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
    fi
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not reachable. Start Docker (or Colima) and retry." >&2
    exit 1
  fi
}

configure_platform() {
  if [[ -z "${DOCKER_PLATFORM:-}" ]]; then
    local arch
    arch=$(uname -m)
    if [[ "$arch" == "arm64" || "$arch" == "aarch64" ]]; then
      DOCKER_PLATFORM=linux/amd64
    fi
  fi
}

ensure_docker
configure_platform

if [[ $# -eq 0 ]]; then
  RUN_CMD='set -e; ./gradlew --no-daemon assemble --stacktrace && ./gradlew --no-daemon check --stacktrace'
else
  GRADLE_CMD=("./gradlew" "--no-daemon")
  GRADLE_CMD+=("$@")
  GRADLE_CMD+=("--stacktrace")
  printf -v RUN_CMD '%q ' "${GRADLE_CMD[@]}"
  RUN_CMD="set -e; ${RUN_CMD}"
fi

mkdir -p "$CACHE_DIR"

BUILD_ARGS=()
if [[ -n "${DOCKER_PLATFORM:-}" ]]; then
  BUILD_ARGS+=(--platform "$DOCKER_PLATFORM")
fi
BUILD_ARGS+=(-t "$IMAGE_NAME" -f Dockerfile .)

docker build "${BUILD_ARGS[@]}"

DOCKER_ARGS=(
  --rm
  --user "$(id -u):$(id -g)"
  -e GRADLE_USER_HOME=/workspace/$CACHE_DIR
  -v "$PWD":/workspace
  -w /workspace
)

if [[ -n "${DOCKER_PLATFORM:-}" ]]; then
  DOCKER_ARGS+=(--platform "$DOCKER_PLATFORM")
fi

if [[ -f .env ]]; then
  DOCKER_ARGS+=(--env-file "$PWD/.env")
fi

exec docker run "${DOCKER_ARGS[@]}" "$IMAGE_NAME" bash -lc "$RUN_CMD"
