# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sidekick is a **Kotlin Multiplatform debug overlay SDK** built with **Compose Multiplatform**. It provides a floating debug panel that host apps embed during development. The client app owns the FAB and visibility; Sidekick exposes only the `Sidekick()` composable that renders the menu. In release builds, a noop module strips the overlay entirely. The package namespace is `dev.parez.sidekick`.

**Targets:** Android, iOS (`iosArm64` + `iosSimulatorArm64` — `iosX64` is intentionally not configured; AndroidX Paging 3.5+ no longer publishes that klib), Desktop (JVM), JS browser, WasmJS.

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
│   │   ├── ksp           — JVM-only KSP processor generating type-safe accessors (KotlinPoet)
│   │   └── gradle-plugin — Wires the KSP processor + commonMain srcDir into consuming modules
│   ├── network-monitor/
│   │   ├── api           — SQLDelight data layer for HTTP traffic recording + Paging
│   │   ├── plugin        — Compose UI + NetworkMonitorPlugin (SidekickPlugin impl)
│   │   └── ktor          — Ktor HttpClientPlugin integration (ktor-client-core is compileOnly)
│   ├── log-monitor/
│   │   ├── api           — SQLDelight data layer for log entries + Paging
│   │   ├── plugin        — Compose UI + LogMonitorPlugin (SidekickPlugin impl)
│   │   └── kermit        — Kermit LogWriter bridge that feeds entries into LogMonitorStore
│   └── custom-screens/
│       └── api           — Plugin that lets host apps register arbitrary screens
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

## Versioning and Publishing

### Versioning model: per-family semver + calendar-versioned BOM

Sidekick uses **per-family semver** coordinated by a **calendar-versioned BOM**. A *family* is a directory containing modules that share internal API surface and therefore must move together. The five families:

| Family root | Members |
|---|---|
| `core/` | `plugin-api`, `runtime`, `noop` |
| `plugins/network-monitor/` | `api`, `plugin`, `ktor` |
| `plugins/log-monitor/` | `api`, `plugin`, `kermit` |
| `plugins/preferences/` | `api`, `ksp`, `gradle-plugin` (included build) |
| `plugins/custom-screens/` | `api` |

Each family root owns a single `version.properties` (e.g. `plugins/network-monitor/version.properties`) with two keys:
- `sdk.version` — semver `MAJOR.MINOR.PATCH`
- `sdk.content.hash` — SHA-256 over the union of every member module's `src/main/` + `src/<sourceSet>Main/` source trees

`SidekickVersionReadConventionPlugin` (build-logic) walks up from each module's projectDir to find the nearest family `version.properties` and applies its `sdk.version` to `project.version`. The BOM (`bom/build.gradle.kts`) declares `api(projects.X)` constraints — Gradle resolves each module's `project.version` transitively into the BOM POM's `dependencyManagement` block. The BOM itself is calendar-versioned: `sidekick.bomVersion` in `gradle.properties` holds `YYYY.MM.DD`.

**Why per-family, not per-module?** Modules within a family have tight intra-family deps (e.g. `network-monitor:plugin` imports types from `network-monitor:api`). Per-module versions would let `:plugin@X` claim compatibility with `:api@X` even when an internal `:api` refactor invalidated the binding. Per-family ensures every sibling at the same version pairs cleanly with every other.

### Day-to-day: bumping module versions

`SidekickVersionUpdateConventionPlugin` (build-logic, applied to the root project) registers two tasks:

```bash
./gradlew updateModuleVersions   # local dev — run before pushing source changes
./gradlew checkModuleVersions    # CI gate — runs on every PR via check-versions.yml
```

`updateModuleVersions` walks every family, recomputes the SHA-256 over its union of source trees, and bumps the family's PATCH on any change. **MAJOR/MINOR are manual** — edit `version.properties` directly and re-run `updateModuleVersions` to refresh the hash. ABI-impacting changes to `core` (which contains `plugin-api`) typically warrant manually bumping the MINOR of every depending family in the same PR — the hash automation doesn't follow inter-family dependencies.

The `checkModuleVersions` task fails the build if any family's hash drifted but PATCH wasn't bumped, or if a family is missing its `version.properties`.

`SidekickVersionReadConventionPlugin`'s configuration-time check additionally fails the build if a module can't find any family `version.properties` walking up to the repo root.

### Cutting a BOM release

The publish workflow (`.github/workflows/publish.yml`) fires on `v*` tag push. It runs two steps with `automaticRelease = false` — both bundles land in staging at central.sonatype.com → Deployments and must be manually promoted.

Standard cut:

1. **Bump BOM version.** Edit `gradle.properties:sidekick.bomVersion` to today's date (`YYYY.MM.DD`). Open PR + merge.
2. **Tag and push.**
   ```bash
   git checkout main && git pull
   git tag v<bomVersion>          # e.g. v2026.06.01
   git push origin v<bomVersion>
   ```
3. **Watch the workflow** at `gh run watch <id> --repo jipariz/sidekick --exit-status`.
4. **Promote in Portal.** Open https://central.sonatype.com → Deployments. Two staging deployments per release (main bundle + gradle-plugin bundle). Click Publish on each. Propagation to `repo1.maven.org` takes 10–30 min.
5. **Create GitHub Release.** `gh release create v<bomVersion> --title "Sidekick BOM <bomVersion>" --notes …` with the resolved per-family versions from the new BOM's POM.

### Publish-workflow architecture

Two distinct upload paths because of how the build is structured:

- **Main build** — `./gradlew publishToMavenCentral` from the repo root. The Vanniktech plugin (`com.vanniktech.maven.publish`, applied via `SidekickKmpLibraryPlugin`, plus directly in `bom/` and `plugins/preferences/ksp/`) bundles all KMP + BOM + KSP artifacts into one zip for the Central Portal API.
- **Included gradle-plugin** — `cd plugins/preferences/gradle-plugin && ../../../gradlew publishToMavenCentral` because the gradle-plugin is a standalone included build (separate Gradle root, invisible to `rootProject.subprojects`). It also uses Vanniktech with `configure(GradlePlugin(...))` for the `java-gradle-plugin` shape, bundling the main `pluginMaven` publication plus per-plugin-id marker publications into a second zip.

The published gradle plugin id is `dev.parez.sidekick.preferences`. The marker artifact's version is **decoupled from the impl** — the impl jar publishes at the preferences-family semver (`<../version.properties>:sdk.version`, currently `0.1.0`), but the plugin-marker pom (the artifact the Gradle plugin DSL resolves) publishes at the calendar **BOM version** (`gradle.properties:sidekick.bomVersion`). The marker's pom declares a `<dependency>` on the impl at its semver.

This split (override in the included gradle-plugin's `build.gradle.kts`, in the `afterEvaluate` block on `*PluginMarkerMaven` publications) lets consumers pin a single calendar coordinate in their version catalog — `sidekick-preferences = { id = "dev.parez.sidekick.preferences", version.ref = "sidekick" }` where `sidekick` is the same BOM version key. They never see the family semver.

### Maven Central namespace + signing

- **Namespace** `dev.parez` is verified on Sonatype Central Portal (`central.sonatype.com → Namespaces`). All artifacts publish to `dev.parez.sidekick:*`.
- **Signing key** GPG, in-memory. Stored in GitHub environment `maven-central` as `SIGNING_IN_MEMORY_KEY` + `SIGNING_IN_MEMORY_KEY_PASSWORD`. Public key uploaded to `keys.openpgp.org` + `keyserver.ubuntu.com`.
- **Portal user token** stored in the same environment as `MAVEN_CENTRAL_USERNAME` + `MAVEN_CENTRAL_PASSWORD` (short opaque strings — not the portal login). Issued at `central.sonatype.com → View Account → Generate User Token`.
- `SidekickPomConfig.kt` and the per-module `mavenPublishing { ... }` blocks apply signing **only** when `ORG_GRADLE_PROJECT_signingInMemoryKey` is present. Local snapshot builds and CI PR checks therefore don't fail without a key.

### Hard-won lessons (don't repeat these mistakes)

- **Portal upload is bundled-zip, not per-file PUT.** Earlier the gradle-plugin module used raw `maven-publish` with a `maven { url = "central.sonatype.com/api/v1/publisher/upload/" }` repository — every PUT returned 404 because the endpoint only accepts a `POST` with a zipped bundle. Fix: always use Vanniktech's `publishToMavenCentral` task (PR #28).
- **Included builds don't inherit `gradle.properties`.** When the publish step runs from `plugins/preferences/gradle-plugin/`, the root `gradle.properties` is invisible. The build script must read its own `version.properties` (or its family file via `../version.properties`) directly. Originally we tried passing `-Psidekick.version=…` from the workflow shell as a workaround, but the inline-read approach is cleaner.
- **Doc-comment lexer doesn't honor backticks.** Kotlin's block-comment scanner matches `*/` literally even inside backticked code in KDoc. A KDoc that mentions `src/*Main/` will close the comment prematurely. Use `src/<sourceSet>Main/` or similar.
- **Tag re-creation after a failed release.** If a publish run fails partway (one bundle uploaded, the other didn't), drop the partial staging in Portal, fix the issue on a PR, merge, then `git push --delete origin v<x>; git tag -d v<x>` and re-tag at the new HEAD. Don't try to amend the original tag — `git tag -f` works locally but tag-force-push is blocked on protected refs and confuses Central if the original deployment already landed.

## Architecture

### Plugin System
Plugins implement `SidekickPlugin` (from `:core:plugin-api`): `id`, `title`, `icon: ImageVector`, `@Composable fun Content()`. Host apps pass a `List<SidekickPlugin>` to `Sidekick()`. The composable renders the debug panel; the host app is responsible for showing/hiding it (FAB, gesture, etc.).

### Navigation
Uses **Material 3 Adaptive** (`ListDetailPaneScaffold`). Plugin list/detail navigation is state-based in `SidekickState` using `selectedPluginId: String?`. The demo-app uses `ListDetailPaneScaffold` + `rememberListDetailPaneScaffoldNavigator` for adaptive list-detail layout.

### Dependency Injection
**Koin** is used at two levels:

1. **Plugin modules** — each stateful plugin owns an isolated `koinApplication {}` singleton (e.g. `NetworkMonitorKoinContext`, `LogMonitorKoinContext` in the respective `:plugins:*/api` modules). The context is never shared with the host app. Pattern:
   - The `api` module registers a `CoroutineScope` + the data store as `single {}` in a core Koin module.
   - The `plugin` module calls `<Name>KoinContext.loadViewModelModule(module)` once on plugin instantiation to register its ViewModel.
   - `Content()` wraps its composable tree in `KoinIsolatedContext(context = <Name>KoinContext.koinApp)` so `koinViewModel()` resolves from the plugin's private graph.
   - Other sibling modules (e.g. `network-monitor:ktor`, `log-monitor:kermit`) access the shared singleton via a `getDefaultStore()` helper on the context object — avoiding a direct Koin dependency in those modules.

2. **demo-app** — uses `KoinIsolatedContext` with its own `AppModule` (Pokémon repository, ViewModels). Isolated from any host-app Koin instance.

ViewModels are provided via `koin-compose-viewmodel`.

### State Management
Pure Compose state: `mutableStateOf` in `SidekickState`. ViewModels use `androidx.lifecycle.viewmodel-compose`.

### Paging (Network + Log monitors)
Both monitor plugins use **AndroidX Paging 3.5.0** (KMP) for their list screens. The store exposes `pagedX(filter: Flow<XFilter>): Flow<PagingData<X>>`, and `filteredCount(filter): Flow<Long>` for the header counter.

- **SQL path (Android / iOS / JVM / JS)**: `Pager(config, factory = { XPagingSource(db, filter) }).flow`. The `XPagingSource` registers a `Query.Listener` on the entity table (table-scoped, not query-scoped), so any insert/update fires `invalidate()` and Paging reloads only the visible page — the perf win vs. the prior `Flow<List<T>>` re-emission of the full 500/1000-row list.
- **WasmJS fallback**: SQLDelight has no wasmJs driver, so the store keeps an in-memory cap-bounded list. The `pagedX` Flow there is `inMemorySnapshot.map { PagingData.from(it.filter(matches), sourceLoadStates = StaticLoadStates) }`. We bypass `Pager` + `PagingSource` on wasmJs because that chain has interop issues (likely `simpleChannelFlow` internals); a static `PagingData.from(...)` works and is fine for a bounded list. `StaticLoadStates` is `LoadStates(NotLoading(endOfPaginationReached=true), ...)` so the LazyPagingItems transitions out of its initial Loading state when items land.
- **Filtering is SQL-side**: search query and method/level filters are WHERE clauses in `selectPagedFiltered{,AllX}` and `countFiltered{,AllX}`. The two `AllX` variants exist because SQLDelight 2.x generates invalid SQL for `IN ()` — when the filter set is empty, the store dispatches to the no-`IN` variant.
- **Filter state lives in the VM**: `MutableStateFlow<String>` for query + `MutableStateFlow<Set<…>>` for level/method chips, `combine(query.debounce(150L), filter).distinctUntilChanged()` → passed to `store.pagedX(filterFlow)` → `cachedIn(viewModelScope)`. `PagingData` is exposed as a **separate** `Flow` on the VM, never wrapped in a UiState (wrapping causes scroll-to-top on any state change).
- **Detail pane is `selectById`-backed**: the entity may not be on a currently-loaded page, so the detail panel collects `store.xById(id).asFlow().mapToOneOrNull()` directly — independent of pagination.

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

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Always use `libs.*` accessors in `build.gradle.kts` — never hardcode versions. Key versions: Kotlin 2.3.20, Compose Multiplatform 1.10.3, AGP 8.13.0, M3 Adaptive 1.2.0, Koin 4.1.1, SQLDelight 2.1.0, Ktor 3.1.3, Room 3 3.0.0-alpha03, AndroidX Paging 3.5.0 (KMP; AOSP coordinates `androidx.paging:paging-{common,compose}`).

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
| Koin | DI (isolated plugin contexts + demo-app) | network-monitor/api+plugin, log-monitor/api+plugin, demo-app |
| SQLDelight | HTTP traffic + log entry DB (generateAsync = true) | network-monitor/api, log-monitor/api |
| AndroidX Paging | Paged list flows in monitor plugins | network-monitor/api+plugin, log-monitor/api+plugin |
| Ktor | HTTP client + interceptor | network-monitor/ktor, demo-app |
| Kermit | Multiplatform logging bridge | log-monitor/kermit, demo-app |
| DataStore | Preferences persistence | preferences/api |
| KSP + KotlinPoet | Code generation for preferences | preferences/ksp |
| Room 3 | Local cache | demo-app |
| Coil 3 | Image loading | demo-app |
