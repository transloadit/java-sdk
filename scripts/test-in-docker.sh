#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME=${IMAGE_NAME:-transloadit-java-sdk-dev}
CACHE_DIR=.gradle-docker

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run this script." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is available but the daemon is not reachable. Start Docker and retry." >&2
  exit 1
fi

if [[ $# -eq 0 ]]; then
  GRADLE_ARGS=(test)
else
  GRADLE_ARGS=("$@")
fi

# Ensure cache directory exists with correct ownership for the mounted volume
mkdir -p "$CACHE_DIR"

# Build the base image (quick when cached)
docker build -t "$IMAGE_NAME" -f Dockerfile .

# Compose the Gradle command preserving argument quoting
GRADLE_CMD=("./gradlew" "--no-daemon")
GRADLE_CMD+=("${GRADLE_ARGS[@]}")

printf -v GRADLE_CMD_STRING '%q ' "${GRADLE_CMD[@]}"

DOCKER_ARGS=(
  --rm
  --user "$(id -u):$(id -g)"
  -e GRADLE_USER_HOME=/workspace/$CACHE_DIR
  -v "$PWD":/workspace
  -w /workspace
)

if [[ -f .env ]]; then
  DOCKER_ARGS+=(--env-file "$PWD/.env")
fi

exec docker run "${DOCKER_ARGS[@]}" "$IMAGE_NAME" bash -lc "$GRADLE_CMD_STRING"
