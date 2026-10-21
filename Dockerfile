# syntax=docker/dockerfile:1

FROM node:26.12.1-bookworm-slim AS node
FROM eclipse-temurin:17-jdk AS base

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       curl \
       unzip \
       ca-certificates \
       gnupg \
       libatomic1 \
    && rm -rf /var/lib/apt/lists/*

# Use the patched official runtime for signature parity tooling.
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules/npm /usr/local/lib/node_modules/npm
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

WORKDIR /workspace
