# Contributing to the Transloadit Java SDK

Thanks for your interest in contributing! This document explains how to get set up, run tests, and how releases are produced.

## Getting Started

1. Fork the repository and clone your fork.
2. Install JDK 8+ (CI runs on Java 8 and 11).
3. Install [Docker](https://docs.docker.com/get-docker/) if you want to mirror the CI environment.
4. Run `./gradlew assemble` to ensure everything compiles.

## Running Tests

We rely on two layers of testing:

- **Host JVM:** `./gradlew check` runs unit and integration tests on your local JDK.
- **Docker (CI parity):** `./scripts/test-in-docker.sh` runs the same Gradle tasks inside the image used in CI. Run this before pushing large changes to double-check parity.

End-to-end tests talk to the live Transloadit API. To enable them locally, create a `.env` file with:

```
TRANSLOADIT_KEY=your-key
TRANSLOADIT_SECRET=your-secret
```

Without these variables the tests are skipped automatically.

## Pull Requests

- Keep PRs focused. For larger refactors, open an issue first to discuss the approach.
- Add or update tests together with code changes.
- Run `./gradlew check` (and optionally the docker script) before submitting a PR.
- Fill in the pull request template with context on the change and testing.

## Publishing Releases

Releases are handled by the Transloadit maintainers through the [release GitHub Action](./.github/workflows/release.yml), which publishes artifacts to [Maven Central](https://search.maven.org/artifact/com.transloadit.sdk/transloadit).

High-level checklist for maintainers:

1. Bump the version in `src/main/resources/java-sdk-version/version.properties` and update `CHANGELOG.md`.
2. Merge the release branch into `main`.
3. Create a git tag for `main` that matches the new versions
4. Publish a GitHub release (include the changelog). This triggers the release workflow. (via the GitHub UI, `gh release creates v1.0.1 --title "v1.0.1" --notes-file <(cat CHANGELOG.md section)`)
5. Wait for Sonatype to sync the artifact (this can take a few hours).

The required signing keys and credentials are stored as GitHub secrets. If you need access or spot an issue with the release automation, please reach out to the Transloadit team via the issue tracker or support channels.

