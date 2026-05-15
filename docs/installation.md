# Installation

Sidekick is a multi-module library published to **Maven Central** under the `dev.parez.sidekick` group. Add the modules you need as dependencies in your app's `build.gradle.kts`.

## Repository

Maven Central is available by default in Gradle. If your project pins a custom repository list, make sure `mavenCentral()` is included:

```kotlin
repositories {
    mavenCentral()
}
```

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
    implementation(platform("dev.parez.sidekick:bom:0.1.0"))

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

The Preferences plugin ships a KSP processor that generates boilerplate from annotations. There are two ways to wire it up.

### Recommended: apply the Sidekick Preferences Gradle plugin

The `dev.parez.sidekick.preferences` Gradle plugin bundles the KSP processor application, the generated-sources directory registration, and the task-dependency wiring — so the entire setup collapses to:

```kotlin
plugins {
    id("dev.parez.sidekick.preferences") version "0.1.0"
}
```

### Manual

If you prefer explicit control, apply the KSP plugin and register the processor yourself:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "dev.parez.sidekick:preferences-ksp:0.1.0")
}

// All compile and KSP tasks must wait for the common-metadata KSP pass
tasks.matching { task ->
    task.name != "kspCommonMainKotlinMetadata" &&
        (task.name.startsWith("compile") && task.name.contains("Kotlin") ||
            task.name.startsWith("ksp"))
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Disable build caching for the KSP task (source dir registration is unreliable in cache)
tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    outputs.cacheIf { false }
    val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
    outputs.upToDateWhen { outDir.get().asFile.exists() }
}
```

!!! tip "Automated setup"
    Use the [`/setup-sidekick`](claude-code-skills.md#setup-sidekick) Claude Code skill to handle all of this automatically, including KSP wiring and plugin selection.
