#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME=${IMAGE_NAME:-transloadit-android-sdk-dev}
CACHE_ROOT=.android-docker
GRADLE_CACHE_DIR="$CACHE_ROOT/gradle"
HOME_DIR="$CACHE_ROOT/home"
ANDROID_SDK_ROOT=/opt/android-sdk

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
  GRADLE_ARGS=(test)
else
  GRADLE_ARGS=("$@")
fi

mkdir -p "$GRADLE_CACHE_DIR" "$HOME_DIR/.android"

GRADLE_CMD=("./gradlew" "--no-daemon")
GRADLE_CMD+=("${GRADLE_ARGS[@]}")
printf -v GRADLE_CMD_STRING %q  "${GRADLE_CMD[@]}"

BUILD_ARGS=()
if [[ -n "${DOCKER_PLATFORM:-}" ]]; then
  BUILD_ARGS+=(--platform "$DOCKER_PLATFORM")
fi
BUILD_ARGS+=(-t "$IMAGE_NAME" -f Dockerfile .)

docker build "${BUILD_ARGS[@]}"

CONTAINER_HOME=/workspace/$HOME_DIR

DOCKER_ARGS=(\
  --rm\
  --user "$(id -u):$(id -g)"\
  -e ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"\
  -e ANDROID_HOME="$ANDROID_SDK_ROOT"\
  -e GRADLE_USER_HOME=/workspace/$GRADLE_CACHE_DIR\
  -e HOME="$CONTAINER_HOME"\
  -v "$PWD":/workspace\
  -w /workspace\
)

if [[ -n "${DOCKER_PLATFORM:-}" ]]; then
  DOCKER_ARGS+=(--platform "$DOCKER_PLATFORM")
fi

if [[ -f .env ]]; then
  DOCKER_ARGS+=(--env-file "$PWD/.env")
fi

HOST_JAVA_SDK="$(cd "$(dirname "$PWD")" && pwd)/java-sdk"
if [[ -d "$HOST_JAVA_SDK" ]]; then
  DOCKER_ARGS+=(-v "$HOST_JAVA_SDK":/workspace/../java-sdk)
fi

exec docker run "${DOCKER_ARGS[@]}" "$IMAGE_NAME" bash -lc "$GRADLE_CMD_STRING"
