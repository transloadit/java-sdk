# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS base

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       curl \
       unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
