# Release Builds

Replace `runtime` with `noop` in release builds. The no-op module replaces `Sidekick()` with an empty composable that does nothing — no panel, no overlay, no overhead.

`Sidekick()` has the same signature in both modules. No code changes required.

## Android (single-module app)

Use Gradle's variant-specific configurations:

```kotlin
// build.gradle.kts (Android app module)
dependencies {
    debugImplementation("dev.parez.sidekick:runtime:0.1.0")
    releaseImplementation("dev.parez.sidekick:noop:0.1.0")
}
```

## Android (multi-module KMP app)

If the module that *calls* `Sidekick()` is a KMP library, you can't put the variant-specific deps there — `debugImplementation` is an Android Gradle Plugin concept that doesn't apply to KMP source sets. Split the wiring across two modules — the library compiles against the API, the Android app module provides the per-variant impl. The full pattern is in [Installation › Multi-module KMP app](installation.md#multi-module-kmp-app).

## iOS

iOS apps have no Gradle-level debug/release split — your Xcode build configuration controls which framework ships. The simplest pattern is to depend on `runtime` in `appleMain` (or `iosMain`) and accept that Sidekick is present in iOS release builds. The panel only renders when you show it, so the user never sees it unless your app deliberately opens it.

If you need iOS release builds to be completely Sidekick-free, set up Xcode-configuration-aware Kotlin source sets — out of scope here, but the standard Compose Multiplatform pattern applies.

## Desktop (JVM) and Web

Same as iOS: there's no Gradle variant split, so depend on `runtime` directly:

```kotlin
jvmMain.dependencies {
    implementation("dev.parez.sidekick:runtime:0.1.0")
}
```

Swap to `dev.parez.sidekick:noop:0.1.0` for production builds yourself — typically via a build flag, a separate distribution task, or a conditional in your build script.

## What gets stripped in `noop`

| Module | Stripped in `noop`? |
|---|---|
| `Sidekick()` composable | ✅ Replaced with an empty composable. |
| Plugin host UI (list / detail navigation, theming) | ✅ Not present. |
| Plugin data layers (NetworkMonitorStore, LogMonitorStore, PreferenceStore) | ❌ See note below. |

Plugin data-layer artifacts (`network-monitor`, `log-monitor`, `preferences`, etc.) are declared as `implementation`, not `compileOnly`. They remain in the runtime classpath in release. If you `install(NetworkMonitorKtor)` or attach `LogMonitorLogWriter()` unconditionally, the data layer still records into in-memory stores in your release build — the panel just doesn't render them.

To keep release builds completely free of recording overhead, gate the install call on build type:

```kotlin
if (!appProperties.isRelease) {
    install(NetworkMonitorKtor) { /* … */ }
}
```

See [Network Monitor › Conditional install](plugins/network-monitor.md#conditional-install-in-release-builds) and [Log Monitor › Conditional bridge](plugins/log-monitor.md#conditional-bridge-in-release-builds).
