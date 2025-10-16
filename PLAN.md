# Workplan: Align Local Docker Tests with CI and Stabilize Integrations

Context: After reintroducing signature provider support and overhauling the Android SDK async layer, CI still fails on java-sdk (Javadoc + Checkstyle) and we found that our Docker helper scripts weren’t mirroring CI behaviour. We’ve been iterating on both repos to make `./scripts/test-in-docker.sh` run the same Gradle targets as CI and expose the same Javadoc/checkstyle failures locally.

## Outstanding Tasks

### java-sdk
- ✅ Fix SignatureProvider Javadoc (done).
- ✅ Add package-info and clear checkstyle warnings for integration tests (done).
- [ ] Re-run docker test (`./scripts/test-in-docker.sh`) after the latest fixes and ensure it runs `assemble` + `check` with `--stacktrace`.
- [ ] Confirm CI after pushes (should now pass once local docker test is clean).

### android-sdk
- ✅ settings.gradle fallback to Git source dependency for java-sdk (done).
- ✅ Docker script updated to run `assemble` + `check`.
- [ ] Docker run still flakes (Gradle daemon “disappeared” when running assemble). Need to investigate (maybe reduce Gradle parallelism or memory?). Perhaps run assemble and check sequentially in separate invocations within same container, or pass `--no-daemon`/`org.gradle.daemon=false` via `GRADLE_OPTS`.
- [ ] Once docker script is stable, ensure CI passes.

### General
- [ ] After stabilizing CI, continue with TODO-V1.md items for Android: main-thread listener, WorkManager story, API polish, docs, release automation.
- [ ] Keep docker scripts almost identical across repos going forward so fixes apply to both.

## Notes for future session
- The Android docker build currently uses the same container to build both java-sdk (composite build) and android modules. The failure seems related to Gradle daemon/FS watchers. Consider setting `org.gradle.daemon=false` or running with `--no-daemon` explicitly (currently already on command). Maybe reduce concurrency with `--max-workers=2`.
- Keep an eye on `.android` analytics warning (Gradle complaining about metrics). Possibly set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or disable analytics via env `ANDROID_SDK_ROOT` etc.
