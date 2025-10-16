# Workplan: Align Local Docker Tests with CI and Stabilize Integrations

Context: After reintroducing signature provider support and overhauling the Android SDK async layer, CI still fails on java-sdk (Javadoc + Checkstyle) and we found that our Docker helper scripts weren’t mirroring CI behaviour. We’ve been iterating on both repos to make `./scripts/test-in-docker.sh` run the same Gradle targets as CI and expose the same Javadoc/checkstyle failures locally.

## Outstanding Tasks

### java-sdk
- ✅ Fix SignatureProvider Javadoc (done).
- ✅ Add package-info and clear checkstyle warnings for integration tests (done).
- [x] Re-run docker test (`./scripts/test-in-docker.sh`) after the latest fixes and ensure it runs `assemble` + `check` with `--stacktrace` (passes locally on Oct 16, 2025).
- [ ] Confirm CI after pushes (should now pass once local docker test is clean).

### android-sdk
- ✅ settings.gradle fallback to Git source dependency for java-sdk (done).
- ✅ Docker script updated to run `assemble` + `check`.
- [ ] Docker run still fails: `./scripts/test-in-docker.sh check` dies on `:examples:generateDebugAndroidTestLintModel` because the composite build cannot resolve `/java-sdk/build/libs/transloadit-2.1.0.jar`. Need to ensure the included java-sdk artifact is built/accessible (maybe trigger `:java-sdk:assemble` within the container, adjust lint inputs, or point lint to the workspace copy).
- [ ] Once docker script is stable, ensure CI passes.

### General
- [ ] After stabilizing CI, continue with TODO-V1.md items for Android: main-thread listener, WorkManager story, API polish, docs, release automation.
- [ ] Keep docker scripts almost identical across repos going forward so fixes apply to both.

## Notes for future session
- Android docker build currently fails because lint cannot locate the included java-sdk jar when generating the debug AndroidTest model. Investigate running `./gradlew :java-sdk:assemble` first, wiring lint to the workspace output, or disabling that lint variant in Docker builds.
- Keep an eye on `.android` analytics warning (Gradle complaining about metrics). Possibly set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or disable analytics via env `ANDROID_SDK_ROOT` etc.
