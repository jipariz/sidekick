# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sidekick is a **Kotlin Multiplatform debug overlay SDK** built with **Compose Multiplatform**. It provides a floating debug panel that host apps embed during development. The client app owns the FAB and visibility; Sidekick exposes only the `Sidekick()` composable that renders the menu. In release builds, a noop module strips the overlay entirely. The package namespace is `dev.parez.sidekick`.

**Targets:** Android, iOS (arm64 + simulator), Desktop (JVM), JS browser, WasmJS.

## Module Structure

```
settings.gradle.kts
├── core/
│   ├── plugin-api        — SidekickPlugin interface, shared types, SidekickInitializer ContentProvider
│   ├── runtime           — Full overlay: Sidekick composable, SidekickState, navigation, theme
│   └── noop              — Release stub: Sidekick() is a no-op (zero overhead)
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

# Publish to Maven Local
./gradlew publishToMavenLocal --no-configuration-cache
```

iOS: open `iosApp/` in Xcode or use an IDE run configuration.

## Architecture

### Plugin System
Plugins implement `SidekickPlugin` (from `:core:plugin-api`): `id`, `title`, `icon: ImageVector`, `@Composable fun Content()`. Host apps pass a `List<SidekickPlugin>` to `Sidekick()`. The composable renders the debug panel; the host app is responsible for showing/hiding it (FAB, gesture, etc.).

### Navigation
Uses **Material 3 Adaptive** (`ListDetailPaneScaffold`). Plugin list/detail navigation is state-based in `SidekickState` using `selectedPluginId: String?`. The demo-app uses `ListDetailPaneScaffold` + `rememberListDetailPaneScaffoldNavigator` for adaptive list-detail layout.

### Dependency Injection
**Koin** is used at two levels:

1. **Plugin modules** — each stateful plugin owns an isolated `koinApplication {}` singleton (e.g. `NetworkMonitorKoinContext` in `:plugins:network-monitor:api`). The context is never shared with the host app. Pattern:
   - The `api` module registers a `CoroutineScope` + the data store as `single {}` in a core Koin module.
   - The `plugin` module calls `<Name>KoinContext.loadViewModelModule(module)` once on plugin instantiation to register its ViewModel.
   - `Content()` wraps its composable tree in `KoinIsolatedContext(context = <Name>KoinContext.koinApp)` so `koinViewModel()` resolves from the plugin's private graph.
   - Other sibling modules (e.g. `network-monitor:ktor`) access the shared singleton via a `getDefaultStore()` helper on the context object — avoiding a direct Koin dependency in those modules.

2. **demo-app** — uses `KoinIsolatedContext` with its own `AppModule` (Pokémon repository, ViewModels). Isolated from any host-app Koin instance.

ViewModels are provided via `koin-compose-viewmodel`.

### State Management
Pure Compose state: `mutableStateOf` in `SidekickState`. ViewModels use `androidx.lifecycle.viewmodel-compose`.

### Android Context Initialization
`SidekickInitializer` is a `ContentProvider` in `:core:plugin-api` that auto-initializes `ApplicationContextHolder` at app startup — no manual setup required in consuming apps. `ApplicationContextHolder.isInitialized` guards against uninitialized access for consumers that don't go through the normal ContentProvider path.

### Debug vs Release
- `debugImplementation(projects.core.runtime)` — full overlay
- `releaseImplementation(projects.core.noop)` — no-op, zero cost
- JVM desktop: add `jvmMain.dependencies { implementation(projects.core.runtime) }` separately (`debugImplementation` is Android-only)

### Theming
`Sidekick()` accepts `useSidekickTheme: Boolean = true`:
- `true` → applies the library's own light/dark Material 3 color scheme based on system dark-mode
- `false` → inherits the host app's ambient `MaterialTheme` as-is

## Build-Logic Convention Plugin

`sidekick.kmp.library` (`SidekickKmpLibraryPlugin` in `build-logic/`) auto-configures: `kotlin-multiplatform`, `com.android.library`, `compose`, `kotlin.plugin.compose`, `maven-publish`. Sets all KMP targets, Java 11, `publishLibraryVariants("release", "debug")` for Android AAR publishing, and injects Compose runtime/foundation/material3/ui into `commonMain`. Used by all `core/*` and `plugins/**` modules.

## Dependency Management

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Always use `libs.*` accessors in `build.gradle.kts` — never hardcode versions. Key versions: Kotlin 2.3.20, Compose Multiplatform 1.10.3, AGP 8.13.0, M3 Adaptive 1.2.0, Koin 4.1.1, SQLDelight 2.1.0, Ktor 3.1.3, Room 3 3.0.0-alpha03.

Gradle configuration cache and build caching are both enabled (`gradle.properties`). JVM heap: Gradle daemon 4 GB, Kotlin daemon 3 GB.

## KSP + KMP Setup

The preferences KSP processor (`:plugins:preferences:ksp`) is JVM-only. In consuming modules:
- Only use `kspCommonMainMetadata` configuration (not per-target `kspAndroid`/`kspJvm`)
- Wire generated sources: `commonMain.kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))`
- All `compile*` and `ksp*` tasks must `dependsOn("kspCommonMainKotlinMetadata")`

## Key Libraries

| Library | Purpose | Module(s) |
|---------|---------|-----------|
| M3 Adaptive | List-detail navigation | runtime, network-monitor, log-monitor, demo-app |
| Koin | DI (isolated plugin contexts + demo-app) | network-monitor/api, network-monitor/plugin, demo-app |
| SQLDelight | HTTP traffic DB | network-monitor/api |
| Ktor | HTTP client + interceptor | network-monitor/ktor, demo-app |
| DataStore | Preferences persistence | preferences/api |
| KSP + KotlinPoet | Code generation for preferences | preferences/ksp |
| Room 3 | Local cache | demo-app |
| Coil 3 | Image loading | demo-app |
