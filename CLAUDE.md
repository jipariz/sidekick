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
│   ├── shell             — Full overlay shell: Sidekick composable, SidekickState, navigation, theme
│   └── noop              — Release stub: Sidekick() is a no-op (zero overhead)
├── plugins/
│   ├── preferences/
│   │   ├── api           — DataStore-backed preferences UI (@SidekickPreferences / @Preference / @IgnorePreference annotations)
│   │   ├── ksp           — JVM-only KSP processor generating type-safe accessors (KotlinPoet). Reads property defaults from each property's Kotlin initializer via source-location lookup.
│   │   └── gradle-plugin — Applies KSP, registers the generated-sources srcDir, wires task deps. Does NOT auto-add the preferences-ksp dependency — consumers do that on `kspCommonMainMetadata` themselves.
│   ├── network-monitor/
│   │   ├── api           — SQLDelight data layer for HTTP traffic recording + Paging
│   │   ├── ui            — Compose UI + NetworkMonitorPlugin (SidekickPlugin impl)
│   │   ├── ktor          — Ktor HttpClientPlugin integration (ktor-client-core is compileOnly)
│   │   └── noop          — Release stub: same FQNs but recordX/install hooks are no-ops, no SQLDelight
│   ├── log-monitor/
│   │   ├── api           — SQLDelight data layer for log entries + Paging
│   │   ├── ui            — Compose UI + LogMonitorPlugin (SidekickPlugin impl)
│   │   ├── kermit        — Kermit LogWriter bridge that feeds entries into LogMonitorStore
│   │   └── noop          — Release stub: LogMonitorLogWriter discards entries, no SQLDelight
│   └── custom-screen/
│       └── api           — CustomScreenPlugin: wraps any Composable as a SidekickPlugin
├── demo/                  — Pokemon catalog sample exercising all SDK features.
│   │                        Follows the new KMP default structure (kmp.new):
│   │                        one shared library + one app module per target.
│   ├── shared/           — KMP library with all cross-platform code + per-target
│   │                       expect/actuals. Room cache on every target — Android
│   │                       & JVM file-backed, iOS file-backed under
│   │                       NSDocumentDirectory, web in-memory via the sqlite-web
│   │                       worker. Built with com.android.kotlin.multiplatform.library.
│   │                       Source-set intermediates:
│   │                         • nonWebMain (android+jvm+ios) — no-op
│   │                           BrowserHistoryEffect actual.
│   │                         • webMain (auto from default hierarchy template) —
│   │                           real BrowserHistoryEffect + navigation3-browser.
│   │                         • nonIosMain (android+jvm+web) — structural; held
│   │                           Room runtime when iOS lacked variants. Now empty
│   │                           but kept for future "everything-except-iOS" deps.
│   ├── androidApp/       — com.android.application shell. Depends on :demo:shared.
│   ├── desktopApp/       — Kotlin/JVM + Compose Desktop. Owns Main.kt + compose.desktop {…}.
│   ├── webApp/           — KMP module with js + wasmJs targets. Owns the web Main.kt
│   │                       + sqlite-worker NPM bundle + webpack.config.d/.
│   └── iosApp/           — Xcode project. Consumes :demo:shared via
│                           embedAndSignAppleFrameworkForXcode. NOT a Gradle module.
└── build-logic/          — Convention plugin: sidekick.kmp.library (SidekickKmpLibraryPlugin)
```

Use typesafe project accessors: `projects.core.shell`, `projects.plugins.preferences.api`, etc.

## Build Commands

```bash
# Demo app — Android
./gradlew :demo:androidApp:assembleDebug

# Demo app — Desktop (JVM, Compose)
./gradlew :demo:desktopApp:run

# Demo app — Web (Wasm)
./gradlew :demo:webApp:wasmJsBrowserDevelopmentRun

# Demo app — Web (JS)
./gradlew :demo:webApp:jsBrowserDevelopmentRun

# Run all tests across all modules
./gradlew allTests

# Run tests for a specific module
./gradlew :core:shell:allTests
./gradlew :demo:shared:jvmTest --tests "dev.parez.sidekick.SomeTest"

# Publish to Maven Local
./gradlew publishToMavenLocal --no-configuration-cache
```

iOS: open `iosApp/` in Xcode or use an IDE run configuration.

## Versioning and Publishing

### Versioning model: per-family semver + calendar-versioned BOM

Sidekick uses **per-family semver** coordinated by a **calendar-versioned BOM**. A *family* is a directory containing modules that share internal API surface and therefore must move together. The five families:

| Family root | Members |
|---|---|
| `core/` | `plugin-api`, `shell`, `noop` |
| `plugins/network-monitor/` | `api`, `ui`, `ktor`, `noop` |
| `plugins/log-monitor/` | `api`, `ui`, `kermit`, `noop` |
| `plugins/preferences/` | `api`, `ksp`, `gradle-plugin` (included build) |
| `plugins/custom-screen/` | `api` |

Each family root owns a single `version.properties` (e.g. `plugins/network-monitor/version.properties`) with two keys:
- `sdk.version` — semver `MAJOR.MINOR.PATCH`
- `sdk.content.hash` — SHA-256 over the union of every member module's `src/main/` + `src/<sourceSet>Main/` source trees

`SidekickVersionReadConventionPlugin` (build-logic) walks up from each module's projectDir to find the nearest family `version.properties` and applies its `sdk.version` to `project.version`. The BOM (`bom/build.gradle.kts`) declares `api(projects.X)` constraints — Gradle resolves each module's `project.version` transitively into the BOM POM's `dependencyManagement` block. The BOM itself is calendar-versioned: `sidekick.bomVersion` in `gradle.properties` holds `YYYY.MM.DD`.

**Why per-family, not per-module?** Modules within a family have tight intra-family deps (e.g. `network-monitor:ui` imports types from `network-monitor:api`). Per-module versions would let `:ui@X` claim compatibility with `:api@X` even when an internal `:api` refactor invalidated the binding. Per-family ensures every sibling at the same version pairs cleanly with every other.

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
- **`publishToMavenLocal` skips gradle-plugin markers.** Running the task from the root only publishes each module's main publication; the included `plugins/preferences/gradle-plugin` build's per-plugin marker publications (`sidekickPreferencesPluginMarkerMaven`, `sidekickPluginMarkerMaven`) aren't wired into the root's task graph, so they stay unpublished. Consumers that pin `plugins { id("dev.parez.sidekick.preferences") version "<bom>" }` then fail to resolve the marker. Workaround: invoke the marker tasks explicitly from the gradle-plugin's included build (`(cd plugins/preferences/gradle-plugin && ../../../gradlew publishSidekickPreferencesPluginMarkerMavenPublicationToMavenLocal publishSidekickPluginMarkerMavenPublicationToMavenLocal)`). The Central Portal path is unaffected — Vanniktech bundles the markers into its zip regardless.
- **AGP 9 + `androidLibrary` DSL accessor shadowing under SQLDelight.** When `sidekick.kmp.library` is applied alongside `app.cash.sqldelight`, the AGP 9 `kotlin { android { … } }` accessor is shadowed by KMP's deprecated `android(name: String, …): KotlinAndroidTarget` member function (Gradle's static accessor generator picks the member over the runtime-injected `android` extension). Use the older-spelling `androidLibrary { … }` accessor instead — same `KotlinMultiplatformAndroidLibraryExtension` type, unambiguous in both single-plugin and SQLDelight-co-applied modules. AGP marks `androidLibrary` deprecated as of 9.1.0-alpha09 but it still works in 9.2.x.
- **AGP 9's KMP library plugin drops `publishLibraryVariants("release", "debug")`.** The new `KotlinMultiplatformAndroidLibraryExtension` doesn't expose it — Android publishes one variant only. The `debugImplementation(real)` / `releaseImplementation(noop)` Android-only recipe Sidekick relied on is gone; both real and noop now ride on a single Android coordinate, and consumers pick via a property-gated swap in each `*Main.dependencies` block (see `### Debug vs Release`). This was the load-bearing public-API change of the AGP 9 migration.
- **`applyDefaultHierarchyTemplate` does not match AGP 9's Android target.** `withAndroidTarget()` in the hierarchy template was added for the legacy `KotlinAndroidTarget` produced by `com.android.library` + `org.jetbrains.kotlin.multiplatform`. AGP 9's `com.android.kotlin.multiplatform.library` produces a `KotlinMultiplatformAndroidLibraryTarget` instead, and the template silently skips it. Wire the `nonIosMain` intermediate manually in modules that need it: `val nonIosMain by creating { dependsOn(commonMain) }; named("androidMain") { dependsOn(nonIosMain) }; …`.
- **Room 3 KMP `expect object` requires an explicit override declaration under Kotlin 2.3.** `expect object PokemonDatabaseConstructor : RoomDatabaseConstructor<PokemonDatabase>` without a body now fails the metadata compile (`is not abstract and does not implement abstract member: fun initialize(): T`). Add `{ override fun initialize(): PokemonDatabase }` to the expect declaration — the actual generated by Room's KSP per target then matches up correctly.
- **Preferences plugin's KSP src-dir mirror needs move-semantics under Kotlin 2.3 / Gradle 9.4.** `compileCommonMainKotlinMetadata` now scans every commonMain srcDir, so KSP's auto-registered output dir AND the `syncSidekickPreferencesKsp` mirror both surface the same generated classes. The `Sync` task in `SidekickPreferencesPlugin` therefore wipes the source dir after mirroring (move-semantics) — `kspCommonMainKotlinMetadata` is already configured no-cache / no-up-to-date so the source repopulates each build.
- **Kotlin 2.2-era klibs reference `kotlin.wasm.internal.externrefToBoolean`, which Kotlin 2.3 doesn't export.** Adding `adaptive-navigation3:1.3.0-beta01` to `:demo:shared` produced a runtime `WebAssembly.instantiate(): Import #N "js_code" "kotlin.wasm.internal.externrefToBoolean": function import requires a callable` on every page load. The culprit klibs (abi 2.2.0): AOSP `androidx.lifecycle:lifecycle-{runtime,common}:2.10.0`, `androidx.savedstate:savedstate-{,compose}:1.4.0`, `androidx.navigationevent:navigationevent:1.0.2`, JB-mirror `lifecycle-viewmodel-compose:2.10.0`, JB-mirror `navigationevent-compose:1.0.1`. Force-resolve to their 2.3.20 (abi 2.3.0) rebuilds — see "Dependency Management" above for the exact pins. JB-mirror `savedstate:1.3.6` stays at abi 1.201.0 but its klib is a stub with no bytecode, so it doesn't trip the linker.
- **`androidx.sqlite:sqlite-web:2.7.0-alpha05` lies about `isNull` for any column.** The Wasm worker (`demo/webApp/sqlite-worker/worker.js:65`) samples `sqlite3_column_type(...)` from the FIRST row only and caches it per statement. `WebWorkerSQLiteStatement.isNull(idx)` reads back from that single cached IntArray, so it returns the type of row 0 for every subsequent row. If row 0 has a non-null Int in a nullable column and row 1 has NULL, Room's generated reader skips its null-check (because isNull says false), calls `getLong`, and `StatementResult.getLong:38` NPEs on the JS-side null cell. Workaround: model the schema with NO nullable columns. The Pokemon cache splits `pokemon_list` (id, name) and `pokemon_detail` (id, name, height, weight, *Json) — both NOT NULL throughout. Absence of a `pokemon_detail` row signals "detail not fetched yet"; `observeDetail(id)` returns `Flow<PokemonDetailEntity?>` so Room emits `null` without reading any column.
- **nav3 list-detail back-to-list pattern: replace, don't push.** Default `backStack.add(detail)` stacks every detail, so Back walks A→B→C→B→A→List. For the list-detail UX where Back from any detail should land on the list, detect when the top is already a detail and assign: `if (backStack.lastOrNull() is PokemonDetailKey) backStack[backStack.lastIndex] = newKey else backStack.add(newKey)`. SnapshotStateList's `set(i, x)` is atomic — one snapshot mutation, observable as a single emission by `ChronologicalBrowserNavigation`'s snapshotFlow on web.
- **`@Preview` in commonMain is `androidx.compose.ui.tooling.preview.Preview`, not `org.jetbrains.compose...`.** The CMP artifact `org.jetbrains.compose.ui:ui-tooling-preview` re-publishes the AndroidX classes for non-JVM targets — the *artifact* is JB-namespaced, but the *package* inside is the AndroidX one. IDE auto-import sometimes adds the (non-existent) `org.jetbrains.compose.ui.tooling.preview.Preview` from a hopeful auto-completion guess; only the AndroidX import resolves.
- **AGP 9's `com.android.kotlin.multiplatform.library` doesn't expose `debugImplementation`.** The standard preview wiring (`debugImplementation("…:ui-tooling")`) fails on KMP-library modules. The official KMP docs recipe is to attach `ui-tooling` to the Android runtime classpath via `add("androidRuntimeClasspath", libs.compose.ui.tooling)` in a root-level `dependencies { }` block. For an `application` module (e.g. `:demo:androidApp`), the regular `debugImplementation` form still applies.
- **Studio's "Run on Device" preview action requires an `application` module.** `@Preview` composables inside `:demo:shared` (a `com.android.kotlin.multiplatform.library`) error with "Cannot obtain the package" when you click the gutter "Run on Device" icon — Studio can't derive an applicationId from a library. Mirror the previews in `:demo:androidApp` (`src/main/.../*Previews.kt`) so Studio has a packageable target. The original commonMain previews still render in the IDE preview pane.

## Architecture

### Plugin System
Plugins implement `SidekickPlugin` (from `:core:plugin-api`): `id`, `title`, `icon: ImageVector`, `@Composable fun Content()`. Host apps pass a `List<SidekickPlugin>` to `Sidekick()`. The composable renders the debug panel; the host app is responsible for showing/hiding it (FAB, gesture, etc.).

### Navigation

**Plugin shell (`core/shell`)** — `Sidekick()` uses Material 3 Adaptive's
`ListDetailPaneScaffold` + `rememberListDetailPaneScaffoldNavigator`. Plugin
list/detail navigation is state-based in `SidekickState` via
`selectedPluginId: String?`. No third-party nav library — the navigator is
the source of truth and the host app cannot drive it externally.

**Demo (`demo/shared`)** — uses **AndroidX Navigation 3** with the Material 3
adaptive scene strategy (`adaptive-navigation3`). A single
`NavBackStack<NavKey>` (`rememberNavBackStack(DemoSavedStateConfiguration,
PokemonListKey)`) drives three destinations:

- `PokemonListKey` — list pane (with `detailPlaceholder` for empty state).
- `PokemonDetailKey(id, name)` — detail pane; `metadata = ListDetailSceneStrategy.detailPane()`.
- `SidekickKey` — no list/detail metadata, so it falls through to the default
  single-pane scene and renders full-bleed over whichever pane was active.

The list-detail "pick another from the list while a detail is open" pattern
**replaces** the top of the backstack rather than pushing
(`backStack[backStack.lastIndex] = newKey` when the top is already a
`PokemonDetailKey`). That keeps back returning to the list, not to the previous
detail — the expected list/detail UX. On web the same backstack is bound to
the browser History API; see [Web browser history](#web-browser-history).

`DemoSavedStateConfiguration` registers each `NavKey` subclass in a
`SavedStateConfiguration { serializersModule { polymorphic(NavKey::class) { … } } }`.
Required on non-Android targets — without it `rememberNavBackStack` throws.

### Web browser history

`navigation3-browser` (`com.github.terrakok:navigation3-browser:1.0.0`,
JS + wasmJs only) wires a nav3 backstack to the browser's History API:
in-app navigation pushes URL fragments; browser Back / Forward / deep-link
loads write back into the backstack. Wired in
`demo/shared/src/webMain/.../navigation/BrowserHistoryEffect.kt` via the
`ChronologicalBrowserNavigation(backStack, saveKey, restoreKey)` overload
that accepts a `NavBackStack<T : NavKey>`.

`saveKey`/`restoreKey` map each destination to a fragment:

- `PokemonListKey` ↔ `#home`
- `PokemonDetailKey(id, name)` ↔ `#pokemon?id=…&name=…`
- `SidekickKey` ↔ `#sidekick`

`restoreKey` for empty hash returns `PokemonListKey` (NOT `null`) — the lib
treats a `null` return as a parse failure and refuses to mutate the
backstack on browser back/forward, so every key reachable on the stack
must round-trip to a non-null value.

The expect/actual signature is `BrowserHistoryEffect(backStack: NavBackStack<NavKey>)`,
no-op on `nonWebMain` (android + jvm + ios), real on `webMain`.

### Dependency Injection
**Koin** is used at two levels:

1. **Plugin modules** — each stateful plugin owns an isolated `koinApplication {}` singleton (e.g. `NetworkMonitorKoinContext`, `LogMonitorKoinContext` in the respective `:plugins:*/api` modules). The context is never shared with the host app. Pattern:
   - The `api` module registers a `CoroutineScope` + the data store as `single {}` in a core Koin module.
   - The `ui` module calls `<Name>KoinContext.loadViewModelModule(module)` once on plugin instantiation to register its ViewModel.
   - `Content()` wraps its composable tree in `KoinIsolatedContext(context = <Name>KoinContext.koinApp)` so `koinViewModel()` resolves from the plugin's private graph.
   - Other sibling modules (e.g. `network-monitor:ktor`, `log-monitor:kermit`) access the shared singleton via a `getDefaultStore()` helper on the context object — avoiding a direct Koin dependency in those modules.

2. **demo/shared** — uses `KoinIsolatedContext` with its own `AppModule` (Pokémon repository, ViewModels). Isolated from any host-app Koin instance.

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

**AGP 9 change (BREAKING):** `com.android.kotlin.multiplatform.library` no longer exposes
`publishLibraryVariants("release", "debug")`. Android consumers can no longer pick
real-vs-noop via `debugImplementation` / `releaseImplementation` — the library
publishes a single Android variant. The recipe is now consumer-driven on **every
target including Android**, mirroring what non-Android already required.

```kotlin
val noopMonitors = (findProperty("sidekick.noop") as? String).toBoolean()

// commonMain — compileOnly gives commonMain the types for compilation only;
// the real module never lands on the runtime classpath when the noop swap is
// active, so it can't collide with the noop variant.
compileOnly(projects.core.shell)
compileOnly(projects.plugins.networkMonitor.ui)
compileOnly(projects.plugins.networkMonitor.ktor)
compileOnly(projects.plugins.logMonitor.ui)
compileOnly(projects.plugins.logMonitor.kermit)

// Per-target — pick real OR noop based on the sidekick.noop Gradle property.
// Repeat the same shape for iosMain, jsMain, wasmJsMain.
androidMain.dependencies {
    if (noopMonitors) {
        implementation(projects.core.noop)
        implementation(projects.plugins.networkMonitor.noop)
        implementation(projects.plugins.logMonitor.noop)
    } else {
        implementation(projects.core.shell)
        implementation(projects.plugins.networkMonitor.ui)
        implementation(projects.plugins.networkMonitor.ktor)
        implementation(projects.plugins.logMonitor.ui)
        implementation(projects.plugins.logMonitor.kermit)
    }
}
jvmMain.dependencies { /* same shape */ }
iosMain.dependencies { /* same shape */ }
jsMain.dependencies { /* same shape */ }
wasmJsMain.dependencies { /* same shape */ }
```

Drive release builds with `./gradlew … -Psidekick.noop=true`. The demo/shared here keeps
unconditional `implementation(real)` on every target — it's a dev-only build, so there's
no prod variant to swap to.

In the noop variant, `NetworkMonitorPlugin(...)` / `LogMonitorPlugin(...)` constructors
are empty (no `store.init()` → no SQLDelight DB opens), `install(NetworkMonitorKtor) { }`
registers no Ktor hooks, and `LogMonitorLogWriter` discards every entry passed to it.

**Historical note:** before the AGP 9 migration, Android picked real-vs-noop via
`debugImplementation(real)` / `releaseImplementation(noop)` from the library's
debug/release variant publication. That mechanism is gone in AGP 9's KMP library
plugin (see `SidekickKmpLibraryPlugin` for the rationale comment).


### Theming
`Sidekick()` accepts `useSidekickTheme: Boolean = true`:
- `true` → applies the library's own light/dark Material 3 color scheme based on system dark-mode
- `false` → inherits the host app's ambient `MaterialTheme` as-is

## Build-Logic Convention Plugin

`sidekick.kmp.library` (`SidekickKmpLibraryPlugin` in `build-logic/`) auto-configures:
`kotlin-multiplatform`, `com.android.kotlin.multiplatform.library` (the AGP 9+
replacement for the old `com.android.library` + `kotlin.multiplatform` combo,
which AGP 9 explicitly disallows), `compose`, `kotlin.plugin.compose`,
`maven-publish`. Sets all KMP targets, configures `compileSdk` / `minSdk` via the
`KotlinMultiplatformAndroidLibraryExtension` (`androidLibrary { … }`), and injects
Compose runtime/foundation/material3/ui into `commonMain`. Used by all `core/*`
and `plugins/**` modules.

## Dependency Management

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Always use `libs.*` accessors in `build.gradle.kts` — never hardcode versions. Key versions (as of 2026-05-23): Kotlin 2.3.21, Compose Multiplatform 1.11.0, AGP 9.2.1, M3 Adaptive 1.3.0-beta01, Koin 4.2.1, SQLDelight 2.3.2, Ktor 3.5.0, Room 3 3.0.0-alpha05, AndroidX Paging 3.5.0 (KMP; AOSP coordinates `androidx.paging:paging-{common,compose}`), AndroidX Navigation 3 1.1.2, `compose-adaptive-navigation3` 1.3.0-beta01, `navigation3-browser` 1.0.0.

The `:demo:shared` module force-bumps several AOSP artifacts past the
versions transitively pulled by `adaptive-navigation3:1.3.0-beta01` — the
older `lifecycle:2.10.0`, `savedstate:1.4.0`, `navigationevent:1.0.2`,
`navigationevent-compose:1.0.1` klibs were compiled with Kotlin 2.2 (abi
2.2.0) and reference `kotlin.wasm.internal.externrefToBoolean`, which
Kotlin 2.3 no longer exports — Wasm instantiation fails with "function
import requires a callable". Forced replacements: `lifecycle:2.11.0-beta02`,
JB-mirror `lifecycle:2.11.0-beta01`, `savedstate:1.5.0`,
`navigationevent:1.1.1`, JB-mirror `navigationevent-compose:1.1.0`. See the
`webMain.dependencies` block in `demo/shared/build.gradle.kts`.

Gradle configuration cache and build caching are both enabled (`gradle.properties`). JVM heap: Gradle daemon 4 GB, Kotlin daemon 3 GB.

## KSP + KMP Setup

The preferences KSP processor (`:plugins:preferences:ksp`) is JVM-only. The `dev.parez.sidekick.preferences` Gradle plugin handles all of the wiring below for KMP consumers; this section is what it does internally (and what a manual setup needs to do).

- Only use `kspCommonMainMetadata` configuration (not per-target `kspAndroid`/`kspJvm`)
- Wire generated sources: `commonMain.kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))`
- All `compile*` and `ksp*` tasks must `dependsOn("kspCommonMainKotlinMetadata")`
- The Gradle plugin does NOT auto-add the `preferences-ksp` artifact — consumers add `kspCommonMainMetadata("dev.parez.sidekick:preferences-ksp:<family-version>")` themselves. This avoids monorepo conflicts and matches the explicit-`ksp(…)` pattern from `teogor/prefero`.

### Room KSP — every target

The demo wires Room KSP per-target (`kspAndroid` / `kspJvm` / `kspJs` /
`kspWasmJs` / `kspIosArm64` / `kspIosSimulatorArm64`) — Room 3.0.0-alpha05
publishes KMP variants for all six. Native targets (js / wasmJs / iOS) need
the generated dirs added to their source set explicitly because KMP KSP
doesn't auto-register them:

```kotlin
val iosArm64Main by getting {
    kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/iosArm64/iosArm64Main/kotlin"))
}
// same shape for iosSimulatorArm64Main, jsMain, wasmJsMain
```

Android and JVM auto-discover the generated dirs via AGP / the JVM compile
task; only the non-JVM targets need the explicit srcDir.

### Reading property defaults

The processor reads each property's Kotlin initializer (`= …`) directly from the source file using `KSPropertyDeclaration.location` (which resolves to a `FileLocation` for normal user-written classes). It parses simple literals (`true`/`false`, integer / long / float / double / string literals, `EnumType.ENTRY` or bare `ENTRY` references) and falls back to the type-zero value (`false`, `0`, `""`, or `enumValues.first()`) when the initializer is missing or unparseable. This means `@Preference(defaultValue = "…")` is **no longer supported** — defaults live in the Kotlin code alongside the property.

## Key Libraries

| Library | Purpose | Module(s) |
|---------|---------|-----------|
| M3 Adaptive (`ListDetailPaneScaffold`) | List-detail layout | shell, network-monitor, log-monitor |
| AndroidX Navigation 3 + `adaptive-navigation3` | Backstack + adaptive scene strategy | demo/shared |
| `navigation3-browser` (terrakok) | Browser History ↔ NavBackStack on web | demo/shared (webMain) |
| Koin | DI (isolated plugin contexts + demo/shared) | network-monitor/api+ui, log-monitor/api+ui, demo/shared |
| SQLDelight | HTTP traffic + log entry DB (generateAsync = true) | network-monitor/api, log-monitor/api |
| AndroidX Paging | Paged list flows in monitor plugins | network-monitor/api+ui, log-monitor/api+ui |
| Ktor | HTTP client + interceptor | network-monitor/ktor, demo/shared |
| Kermit | Multiplatform logging bridge | log-monitor/kermit, demo/shared |
| DataStore | Preferences persistence | preferences/api |
| KSP + KotlinPoet | Code generation for preferences | preferences/ksp |
| Room 3 + `androidx.sqlite` | Local Pokemon cache on every target (Android/JVM/iOS file-backed, web in-memory via sqlite-web worker) | demo/shared |
| Coil 3 | Image loading | demo/shared |
