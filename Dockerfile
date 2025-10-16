# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS base

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       curl \
       unzip \
       ca-certificates \
       gnupg \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 24 (LTS)
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
