# Installation

Sidekick is a multi-module library published to **Maven Central** under the `dev.parez.sidekick` group. Add the modules you need as dependencies in your app's `build.gradle.kts`.

## Versioning at a glance

- **One BOM coordinate covers everything.** The BOM is calendar-versioned (`YYYY.MM.DD`). Pin it once and every Sidekick artifact resolves through it — including `shell` / `noop` in the Android variant-config swap, because BOM constraints propagate down the `implementation` extension chain.
- **Plugin modules are per-family semver under the hood.** Each family (`core`, `network-monitor`, `log-monitor`, `preferences`, `custom-screen`) has its own `MAJOR.MINOR.PATCH` version that can drift independently. You don't need to know these — the BOM pins them.
- **The Gradle plugin tracks the BOM too.** `dev.parez.sidekick.preferences`'s marker artifact is republished at the BOM's calendar version every release, so you pin the same number in `plugins { id("…") version "…" }` (or via `version.ref = "sidekick"` in the catalog). The marker resolves transparently to the impl at its current preferences-family version — you never see that number.

The Maven Central badge at the top of the [README](../README.md) renders the latest BOM coordinate.

## Compatibility

Each Sidekick BOM is built against a fixed Kotlin / CMP / AGP stack. Consumers need to match the Kotlin and Compose Multiplatform minor versions (klib ABI is not stable across minors) and meet the `compileSdk` constraint.

| BOM | Kotlin | Compose Multiplatform | Android `compileSdk` |
|---|---|---|---|
| `2026.05.26` | 2.3.21 | 1.11.0 | **37+** ¹ |
| `2026.05.18` | 2.3.20 | 1.10.3 | 36+       |
| `2026.05.16` | 2.3.20 | 1.10.3 | 36+       |

Android `minSdk` 24+ across all releases.

!!! info "Why `compileSdk ≥ 37` is required for `2026.05.26`"
    `2026.05.26` transitively depends on `androidx.compose.material3.adaptive:*:1.3.0-beta01`, which stamps an AAR-metadata constraint requiring consumers to compile against API 37. Builds below 37 fail fast with `"compile against version 37 or later"`. This affects build-time only — your app's `minSdk` and `targetSdk` are independent and unchanged.

    Older BOMs (`2026.05.18` and earlier) shipped the CMP-namespaced `org.jetbrains.compose.material3.adaptive:adaptive:1.2.0` instead, which has no AAR-metadata constraint.

!!! tip "Kotlin / CMP version drift"
    The matrix lists exact built-with versions. Kotlin/Native klib ABI changes between minors and Compose Multiplatform klib ABI bumps between majors are the usual breakage points — if your consumer is on a different Kotlin minor or a different CMP major than the BOM, expect to encounter `Failed to build cache for … klib` or `unlinked class symbol` errors during the iOS framework-link step.

Full release history and per-version notes: [GitHub releases](https://github.com/jipariz/sidekick/releases).

## Repository

Maven Central is available by default in Gradle. If your project pins a custom repository list, make sure `mavenCentral()` is included:

```kotlin
repositories {
    mavenCentral()
}
```

## Version catalog (copy-paste)

If your project uses a Gradle version catalog (`gradle/libs.versions.toml`), drop the block below in. One BOM version key covers every published Sidekick artifact; the Gradle plugin has its own inline version because the plugin DSL can't resolve through a BOM.

```toml
[versions]
sidekick = "2026.05.17"  # BOM version (YYYY.MM.DD)

[libraries]
sidekick-bom = { module = "dev.parez.sidekick:bom", version.ref = "sidekick" }
# Everything below is BOM-managed — no version needed.
sidekick-shell      = { module = "dev.parez.sidekick:shell" }
sidekick-noop       = { module = "dev.parez.sidekick:noop" }
sidekick-plugin-api = { module = "dev.parez.sidekick:plugin-api" }
sidekick-network-monitor        = { module = "dev.parez.sidekick:network-monitor" }
sidekick-network-monitor-ui = { module = "dev.parez.sidekick:network-monitor-ui" }
sidekick-network-monitor-ktor   = { module = "dev.parez.sidekick:network-monitor-ktor" }
sidekick-network-monitor-noop   = { module = "dev.parez.sidekick:network-monitor-noop" }
sidekick-log-monitor            = { module = "dev.parez.sidekick:log-monitor" }
sidekick-log-monitor-ui     = { module = "dev.parez.sidekick:log-monitor-ui" }
sidekick-log-monitor-kermit     = { module = "dev.parez.sidekick:log-monitor-kermit" }
sidekick-log-monitor-noop       = { module = "dev.parez.sidekick:log-monitor-noop" }
sidekick-preferences            = { module = "dev.parez.sidekick:preferences" }
sidekick-custom-screen         = { module = "dev.parez.sidekick:custom-screen" }

[plugins]
# Preferences KSP wiring — applies the KSP processor + generated-sources srcDir.
# Marker is published at the BOM's calendar version, so we reuse the same key.
sidekick-preferences = { id = "dev.parez.sidekick.preferences", version.ref = "sidekick" }
```

Then reference the typesafe accessors in `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.sidekick.preferences)
}

dependencies {
    debugImplementation(libs.sidekick.shell)
    releaseImplementation(libs.sidekick.noop)

    implementation(platform(libs.sidekick.bom))
    implementation(libs.sidekick.network.monitor.plugin)
    implementation(libs.sidekick.network.monitor.ktor)
    implementation(libs.sidekick.log.monitor.plugin)
    implementation(libs.sidekick.log.monitor.kermit)
    implementation(libs.sidekick.preferences)
    implementation(libs.sidekick.custom.screens)
}
```

The rest of this page uses the inline `"group:artifact:version"` form so it's readable for projects that don't use a version catalog. Both styles work identically.

## Core

Every app needs the core shell (debug builds) and the no-op stub (release builds). How you wire them depends on whether you're in a single-module app or a multi-module one.

### Single-module Android app

```kotlin
// build.gradle.kts (Android app module)
dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.05.17"))
    debugImplementation("dev.parez.sidekick:shell")
    releaseImplementation("dev.parez.sidekick:noop")
}
```

!!! info "`noop`"
    The no-op module replaces `Sidekick()` with an empty composable that does nothing. Zero overhead — Sidekick is completely absent from release builds.

    The network and log monitor plugins ship their own noop modules (`network-monitor-noop`, `log-monitor-noop`) to strip the recording side too. Wire them the same way; see the [Plugins section below](#plugins) and the dedicated [Release builds](release-builds.md) page.

### Multi-module KMP app

`debugImplementation` / `releaseImplementation` are Android Gradle Plugin concepts that don't exist on KMP library source sets. If your feature module (the one that *calls* `Sidekick()`) is a KMP library, you can't put the shell/noop swap there — split it across two modules:

```kotlin
// feature/devtools/build.gradle.kts — KMP library that calls Sidekick()
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:2026.05.17"))
            // compileOnly: the type is on the compile classpath of the library,
            // but the shell impl is provided per-target by the app module below.
            compileOnly("dev.parez.sidekick:shell")
        }
        appleMain.dependencies {
            // iOS has no Gradle-level debug/release split — provide the shell here.
            implementation("dev.parez.sidekick:shell")
        }
    }
}
```

```kotlin
// ATASSproApp/android/build.gradle.kts — Android application module
dependencies {
    implementation(projects.feature.devtools)        // your feature module
    implementation(platform("dev.parez.sidekick:bom:2026.05.17"))
    debugImplementation("dev.parez.sidekick:shell")
    releaseImplementation("dev.parez.sidekick:noop")
}
```

The library compiles against `shell` types; on Android the app module swaps `shell` (debug) for `noop` (release); on iOS the shell ships in both configurations.

### Non-Android targets (Desktop / iOS / JS / Wasm)

`debugImplementation` / `releaseImplementation` are Android Gradle Plugin configurations. Other KMP targets don't have a Gradle-level build-type split, so **the consumer picks the real or noop module manually per build**. The recommended pattern is a property-gated swap — run prod builds with `-Psidekick.noop=true`:

```kotlin
val sidekickNoop = (findProperty("sidekick.noop") as? String).toBoolean()

jvmMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.05.17"))
    if (sidekickNoop) {
        implementation("dev.parez.sidekick:noop")
        implementation("dev.parez.sidekick:network-monitor-noop")
        implementation("dev.parez.sidekick:log-monitor-noop")
    } else {
        implementation("dev.parez.sidekick:shell")
        implementation("dev.parez.sidekick:network-monitor-ui")
        implementation("dev.parez.sidekick:network-monitor-ktor")
        implementation("dev.parez.sidekick:log-monitor-ui")
        implementation("dev.parez.sidekick:log-monitor-kermit")
    }
}
```

Mirror the same shape in `iosMain.dependencies`, `jsMain.dependencies`, and `wasmJsMain.dependencies`. See [Release builds › Non-Android targets](release-builds.md#non-android-targets-ios--desktop-jvm--js--wasm) for the rationale and alternatives (hand-rolled swap, runtime opt-out).

The simplest dev-only setup omits the property entirely and ships the real modules unconditionally on these targets — `Sidekick()` is gated by your app's FAB anyway.

## Plugins

The Sidekick BOM aligns the versions of every plugin module — apply it once and drop the version from individual plugin lines. The network / log monitor plugins follow the same `debugImplementation` ↔ `releaseImplementation` swap as the core, with their own noop modules that strip the SQLDelight recording layer:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:2026.05.17"))

            // Type stubs only — Android's variant swap below provides the real
            // (debug) or noop (release) module on the runtime classpath. Without
            // `compileOnly`, the real plugin would collide with the noop in
            // release.
            compileOnly("dev.parez.sidekick:network-monitor-ui")
            compileOnly("dev.parez.sidekick:network-monitor-ktor")
            compileOnly("dev.parez.sidekick:log-monitor-ui")
            compileOnly("dev.parez.sidekick:log-monitor-kermit")

            // Preferences and Custom Screens have no noop module — they don't
            // record traffic and they don't allocate at construction time.
            implementation("dev.parez.sidekick:preferences")
            implementation("dev.parez.sidekick:custom-screen")
        }
    }
}

dependencies {
    debugImplementation("dev.parez.sidekick:network-monitor-ui")
    debugImplementation("dev.parez.sidekick:network-monitor-ktor")
    releaseImplementation("dev.parez.sidekick:network-monitor-noop")
    debugImplementation("dev.parez.sidekick:log-monitor-ui")
    debugImplementation("dev.parez.sidekick:log-monitor-kermit")
    releaseImplementation("dev.parez.sidekick:log-monitor-noop")
}
```

For non-Android targets, use the property-gated pattern shown above in [Non-Android targets](#non-android-targets-desktop--ios--js--wasm).

## Android Context

On Android, `dev.parez.sidekick:plugin-api` ships a `SidekickInitializer` `ContentProvider` that auto-initializes the library context at app startup. **No manual setup is required** — the `ContentProvider` is merged into the app manifest automatically.

If you need to supply build metadata (build type, flavor) explicitly, you can still call `ApplicationContextHolder.initialize(context)` yourself in `Application.onCreate()`, but it is optional.

## KSP (Preferences code generator)

The Preferences plugin ships a KSP processor that generates boilerplate from `@SidekickPreferences`-annotated classes. Apply the Sidekick Preferences Gradle plugin — it bundles the KSP processor application, the generated-sources directory registration, and the KMP task-ordering wiring:

```kotlin
plugins {
    id("dev.parez.sidekick.preferences") version "2026.05.17"
}
```

That's it. See [Preferences › Defining Preferences](plugins/preferences.md#defining-preferences) for the annotations, and [Preferences › Manual setup without KSP](plugins/preferences.md#manual-setup-without-ksp) if you want to wire the processor by hand instead of applying the Gradle plugin.

!!! tip "Automated setup"
    Use the [`/setup-sidekick`](claude-code-skills.md#setup-sidekick) Claude Code skill to handle the whole install (core, plugins, KSP) in one go.
