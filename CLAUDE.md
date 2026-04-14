# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sidekick is a **Kotlin Multiplatform debug overlay SDK** built with **Compose Multiplatform**. It provides a floating debug panel (FAB + slide-up menu) that host apps embed during development. In release builds, a noop module strips the overlay entirely. The package namespace is `dev.parez.sidekick`.

**Targets:** Android, iOS (arm64 + simulator), Desktop (JVM), JS browser, WasmJS.

## Module Structure

```
settings.gradle.kts
├── core/
│   ├── plugin-api        — SidekickPlugin interface, SidekickColors, shared types
│   ├── runtime           — Full overlay: SidekickShell (FAB + panel), SidekickState, navigation, theme
│   └── noop              — Release stub: SidekickShell is a passthrough (zero overhead)
├── plugins/
│   ├── preferences/
│   │   ├── api           — DataStore-backed preferences UI (@AppPreferences annotation)
│   │   └── ksp           — JVM-only KSP processor generating type-safe accessors (KotlinPoet)
│   └── network-monitor/
│       ├── api           — SQLDelight data layer for HTTP traffic recording
│       ├── plugin        — Compose UI + NetworkMonitorPlugin (SidekickPlugin impl)
│       └── ktor          — Ktor HttpClientPlugin integration (ktor-client-core is compileOnly)
├── demo-app              — Pokemon catalog app exercising all SDK features
├── build-logic/          — Convention plugin: sidekick.kmp.library (SidekickKmpLibraryPlugin)
└── iosApp/               — Xcode project wrapping demo-app for iOS
```

Use typesafe project accessors: `projects.core.runtime`, `projects.plugins.preferences.api`, etc.

## Build Commands

```bash
# Demo app — Android
./gradlew :demo-app:assembleDebug

# Demo app — Desktop (JVM)
./gradlew :demo-app:run

# Demo app — Web (Wasm)
./gradlew :demo-app:wasmJsBrowserDevelopmentRun

# Demo app — Web (JS)
./gradlew :demo-app:jsBrowserDevelopmentRun

# Run all tests across all modules
./gradlew allTests

# Run tests for a specific module
./gradlew :core:runtime:allTests
./gradlew :demo-app:jvmTest --tests "dev.parez.sidekick.SomeTest"
```

iOS: open `iosApp/` in Xcode or use an IDE run configuration.

## Architecture

### Plugin System
Plugins implement `SidekickPlugin` (from `:core:plugin-api`): `id`, `title`, `icon: ImageVector`, `@Composable fun Content()`. Host apps pass a `List<SidekickPlugin>` to `SidekickShell`. The shell renders a FAB; tapping it opens a panel listing available plugins.

### Navigation
Uses **Jetpack Navigation 3** (`androidx.navigation3`). Back stack is a `SnapshotStateList<NavKey>` in `SidekickState`. Nav keys are `@Serializable` data objects/classes. The demo-app uses `NavDisplay` with `ListDetailSceneStrategy` for adaptive list-detail layout.

### Dependency Injection
**Koin** is used in the demo-app. `KoinApplication` starts at the root composable. ViewModels are provided via `koin-compose-viewmodel` (`viewModelOf` / `viewModel { params -> ... }`).

### State Management
Pure Compose state: `mutableStateOf` + `SnapshotStateList` in `SidekickState`. ViewModels use `androidx.lifecycle.viewmodel-compose`.

### Debug vs Release
- `debugImplementation(projects.core.runtime)` — full overlay
- `releaseImplementation(projects.core.noop)` — passthrough, zero cost
- JVM desktop: add `jvmMain.dependencies { implementation(projects.core.runtime) }` separately (`debugImplementation` is Android-only)

## Build-Logic Convention Plugin

`sidekick.kmp.library` (`SidekickKmpLibraryPlugin` in `build-logic/`) auto-configures: `kotlin-multiplatform`, `com.android.library`, `compose`, `kotlin.plugin.compose`. Sets all KMP targets, Java 11, and injects Compose runtime/foundation/material3/ui into `commonMain`. Used by all `core/*` and `plugins/**` modules.

## Dependency Management

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Always use `libs.*` accessors in `build.gradle.kts` — never hardcode versions. Key versions: Kotlin 2.3.20, Compose Multiplatform 1.10.3, AGP 8.13.0, Navigation 3 1.1.0, Koin 4.1.1, SQLDelight 2.1.0, Ktor 3.1.3, Room 3 3.0.0-alpha03.

Gradle configuration cache and build caching are both enabled (`gradle.properties`). JVM heap: Gradle daemon 4 GB, Kotlin daemon 3 GB.

## KSP + KMP Setup

The preferences KSP processor (`:plugins:preferences:ksp`) is JVM-only. In consuming modules:
- Only use `kspCommonMainMetadata` configuration (not per-target `kspAndroid`/`kspJvm`)
- Wire generated sources: `commonMain.kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))`
- All `compile*` and `ksp*` tasks must `dependsOn("kspCommonMainKotlinMetadata")`

## Key Libraries

| Library | Purpose | Module(s) |
|---------|---------|-----------|
| Navigation 3 | Type-safe nav with back stack | runtime, demo-app |
| Koin | DI | demo-app |
| SQLDelight | HTTP traffic DB | network-monitor/api |
| Ktor | HTTP client + interceptor | network-monitor/ktor, demo-app |
| DataStore | Preferences persistence | preferences/api |
| KSP + KotlinPoet | Code generation for preferences | preferences/ksp |
| Room 3 | Local cache | demo-app |
| Coil 3 | Image loading | demo-app |
