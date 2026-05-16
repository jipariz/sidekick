# Installation

Sidekick is a multi-module library published to **Maven Central** under the `dev.parez.sidekick` group. Add the modules you need as dependencies in your app's `build.gradle.kts`.

## Versioning at a glance

- **BOM is calendar-versioned** (`YYYY.MM.DD`). Pin one BOM coordinate and every plugin module is resolved transitively. Cutting a new BOM means publishing a new dated coordinate; older BOMs stay on Central forever.
- **Plugin modules are per-family semver.** Each plugin family — `core` (`plugin-api`/`runtime`/`noop`), `network-monitor`, `log-monitor`, `preferences`, `custom-screens` — has its own `MAJOR.MINOR.PATCH` version. Members of a family always share a version, but families can drift independently.
- **You normally never pin module versions directly** — let the BOM do it. The exceptions are Android's `debugImplementation` / `releaseImplementation` configurations for `core:runtime` and `core:noop`, which don't pick up the BOM and need an explicit `core`-family version.

The Maven Central badge at the top of the [README](../README.md) renders the latest BOM coordinate.

## Repository

Maven Central is available by default in Gradle. If your project pins a custom repository list, make sure `mavenCentral()` is included:

```kotlin
repositories {
    mavenCentral()
}
```

## Version catalog (copy-paste)

If your project uses a Gradle version catalog (`gradle/libs.versions.toml`), drop the block below in. It covers every published Sidekick artifact plus the Preferences Gradle plugin.

```toml
[versions]
# BOM is calendar-versioned — bump when you want to track the latest release.
sidekick-bom = "2026.05.16"
# Core family (plugin-api, runtime, noop). Needed directly because the BOM
# doesn't propagate to Android `debugImplementation`/`releaseImplementation`.
sidekick-core = "0.1.0"

[libraries]
sidekick-bom        = { module = "dev.parez.sidekick:bom",        version.ref = "sidekick-bom" }
sidekick-runtime    = { module = "dev.parez.sidekick:runtime",    version.ref = "sidekick-core" }
sidekick-noop       = { module = "dev.parez.sidekick:noop",       version.ref = "sidekick-core" }
sidekick-plugin-api = { module = "dev.parez.sidekick:plugin-api", version.ref = "sidekick-core" }

# Plugin modules — version pulled from the BOM at use site.
sidekick-network-monitor        = { module = "dev.parez.sidekick:network-monitor" }
sidekick-network-monitor-plugin = { module = "dev.parez.sidekick:network-monitor-plugin" }
sidekick-network-monitor-ktor   = { module = "dev.parez.sidekick:network-monitor-ktor" }
sidekick-log-monitor            = { module = "dev.parez.sidekick:log-monitor" }
sidekick-log-monitor-plugin     = { module = "dev.parez.sidekick:log-monitor-plugin" }
sidekick-log-monitor-kermit     = { module = "dev.parez.sidekick:log-monitor-kermit" }
sidekick-preferences            = { module = "dev.parez.sidekick:preferences" }
sidekick-custom-screens         = { module = "dev.parez.sidekick:custom-screens" }

[plugins]
# Preferences KSP wiring — applies the KSP processor + generated-sources srcDir.
# Gradle plugins resolve via the plugin DSL, not via the BOM, so the version
# is pinned directly here (preferences family version).
sidekick-preferences = { id = "dev.parez.sidekick.preferences", version = "0.1.0" }
```

Then reference the typesafe accessors in `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.sidekick.preferences)
}

dependencies {
    debugImplementation(libs.sidekick.runtime)
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

Every app needs the core runtime (debug builds) and the no-op stub (release builds). How you wire them depends on whether you're in a single-module app or a multi-module one.

### Single-module Android app

```kotlin
// build.gradle.kts (Android app module)
dependencies {
    debugImplementation("dev.parez.sidekick:runtime:0.1.0")
    releaseImplementation("dev.parez.sidekick:noop:0.1.0")
}
```

!!! info "`noop`"
    The no-op module replaces `Sidekick()` with an empty composable that does nothing. Zero overhead — Sidekick is completely absent from release builds.

### Multi-module KMP app

`debugImplementation` / `releaseImplementation` are Android Gradle Plugin concepts that don't exist on KMP library source sets. If your feature module (the one that *calls* `Sidekick()`) is a KMP library, you can't put the runtime/noop swap there — split it across two modules:

```kotlin
// feature/devtools/build.gradle.kts — KMP library that calls Sidekick()
kotlin {
    sourceSets {
        commonMain.dependencies {
            // compileOnly: the type is on the compile classpath of the library,
            // but the runtime impl is provided per-target by the app module below.
            compileOnly("dev.parez.sidekick:runtime:0.1.0")
        }
        appleMain.dependencies {
            // iOS has no Gradle-level debug/release split — provide the runtime here.
            implementation("dev.parez.sidekick:runtime:0.1.0")
        }
    }
}
```

```kotlin
// ATASSproApp/android/build.gradle.kts — Android application module
dependencies {
    implementation(projects.feature.devtools)        // your feature module
    debugImplementation("dev.parez.sidekick:runtime:0.1.0")
    releaseImplementation("dev.parez.sidekick:noop:0.1.0")
}
```

The library compiles against `runtime` types; on Android the app module swaps `runtime` (debug) for `noop` (release); on iOS the runtime ships in both configurations.

### Desktop (JVM)

`debugImplementation` is Android-only, so add the runtime explicitly and swap to `noop` for production builds yourself:

```kotlin
jvmMain.dependencies {
    implementation("dev.parez.sidekick:runtime:0.1.0")
}
```

### iOS

iOS apps don't have a Gradle-level debug/release split — the Xcode build configuration controls which framework ships. Simplest path: depend on `runtime` in `appleMain` (or `iosMain`) and accept that Sidekick is present in iOS release builds. The panel is gated by your app's FAB / visibility logic, so it never appears unless you show it.

If you need iOS release builds to be Sidekick-free, use Xcode configuration-aware source sets (e.g. `iosReleaseMain` depending on `noop`) — out of scope for this guide, but the standard Compose Multiplatform pattern applies.

## Plugins

The Sidekick BOM aligns the versions of every plugin module — apply it once and drop the version from individual plugin lines:

```kotlin
commonMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.05.16"))

    // Network monitor
    implementation("dev.parez.sidekick:network-monitor-plugin")
    implementation("dev.parez.sidekick:network-monitor-ktor")   // Ktor integration

    // Log monitor
    implementation("dev.parez.sidekick:log-monitor-plugin")
    implementation("dev.parez.sidekick:log-monitor-kermit")     // Kermit bridge (optional)

    // Preferences
    implementation("dev.parez.sidekick:preferences")

    // Custom screens
    implementation("dev.parez.sidekick:custom-screens")
}
```

## Android Context

On Android, `dev.parez.sidekick:plugin-api` ships a `SidekickInitializer` `ContentProvider` that auto-initializes the library context at app startup. **No manual setup is required** — the `ContentProvider` is merged into the app manifest automatically.

If you need to supply build metadata (build type, flavor) explicitly, you can still call `ApplicationContextHolder.initialize(context)` yourself in `Application.onCreate()`, but it is optional.

## KSP (Preferences code generator)

The Preferences plugin ships a KSP processor that generates boilerplate from `@SidekickPreferences`-annotated classes. Apply the Sidekick Preferences Gradle plugin — it bundles the KSP processor application, the generated-sources directory registration, and the KMP task-ordering wiring:

```kotlin
plugins {
    id("dev.parez.sidekick.preferences") version "0.1.0"
}
```

That's it. See [Preferences › Defining Preferences](plugins/preferences.md#defining-preferences) for the annotations, and [Preferences › Manual setup without KSP](plugins/preferences.md#manual-setup-without-ksp) if you want to wire the processor by hand instead of applying the Gradle plugin.

!!! tip "Automated setup"
    Use the [`/setup-sidekick`](claude-code-skills.md#setup-sidekick) Claude Code skill to handle the whole install (core, plugins, KSP) in one go.
