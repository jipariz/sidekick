# Release Builds

Replace `core:runtime` with `core:noop` in release builds. The no-op implementation replaces `SidekickShell` with a composable that simply renders `content()` — no FAB, no panel, no overhead:

```kotlin
// build.gradle.kts (Android)
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}
```

For non-Android targets, swap manually or via a build flag:

```kotlin
// build.gradle.kts (Desktop/other)
jvmMain.dependencies {
    implementation(projects.core.runtime) // swap to core:noop for production builds
}
```

No code changes required — `SidekickShell` has the same signature in both modules.
