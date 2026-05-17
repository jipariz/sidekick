---
name: setup-sidekick
description: >
  Interactive setup wizard for adding Sidekick to a consumer app. Handles
  fresh installation (core dependencies, Sidekick composable wiring with FAB
  visibility toggle), plugin selection (network-monitor, log-monitor,
  preferences, custom-screens), and migration of an existing DataStore-based
  preferences class to the Preferences plugin with KSP code generation.
  Trigger with: "set up Sidekick", "add Sidekick", "install Sidekick",
  "migrate preferences to Sidekick", or just "/setup-sidekick".
argument-hint: "[path/to/app/build.gradle.kts]"
allowed-tools: Read Write Edit Bash Glob Grep AskUserQuestion
---

# Sidekick Setup & Preferences Migration Wizard

You are setting up Sidekick in a consumer project. Work through the phases
below in order. Adapt to what you find — skip steps that are already done.

The host owns visibility: Sidekick exposes a `Sidekick(plugins = …)` composable
that renders the debug panel; the host wraps it in a FAB + `AnimatedVisibility`
pair. There is no `SidekickShell` wrapper. Do not invent one.

---

## Phase 1 — Discover

Find the app module's `build.gradle.kts`. If `$ARGUMENTS` is a path, use it.
Otherwise, search for `build.gradle.kts` files that look like app modules
(containing `com.android.application` or a `compose.desktop` block).

Read the file and extract:

- **Build type** — is this a Sidekick mono-repo sub-module (consume via
  `projects.core.runtime`) or an external KMP project that depends on Sidekick
  via Maven coordinates (`dev.parez.sidekick:runtime:<version>`)? Default to
  Maven coordinates unless the project is inside the Sidekick repo (look for a
  sibling `core/runtime/` directory at the repo root) — most consumers are
  external.
- **Targets declared** — `androidTarget`, `jvm()`, `js`, `wasmJs`, iOS.
- **Existing Sidekick dependencies** — any `dev.parez.sidekick:*` or `projects.core.*`.
- **KSP plugin** — is `alias(libs.plugins.ksp)` (or `id("com.google.devtools.ksp")`) already applied?
- **`mavenLocal()` / Maven Central availability** — check the root `settings.gradle.kts` for the `dependencyResolutionManagement.repositories` block. If neither contains Maven Central nor `mavenLocal`, Sidekick artifacts will not resolve.
- **Root composable file** — search for a `@Composable fun` that contains
  `MaterialTheme` or is the app entry point (commonly `App.kt`, `DemoApp.kt`,
  `MainActivity.kt`). Read it to understand the current shape.
- **Existing FAB / debug UI** — if the host already has a FAB, plan to add a second one or reuse it with a long-press / shake gesture (the host decides).

Also search `commonMain` sources for a class that:
- Has `DataStore<Preferences>` as a constructor parameter, **or**
- Uses `dataStore.getFlow(...)`, `dataStore.getBlocking(...)`, `dataStore.updateValue(...)`, **or**
- Is already annotated with `@SidekickPreferences`.

Record the file path and class name if found.

---

## Phase 2 — Ask the user

Ask two questions in a single prompt:

1. **Which plugins do you want?** List options with a one-line description each:

   | # | Plugin | What it does |
   |---|--------|--------------|
   | 1 | Network Monitor | Captures & displays all HTTP traffic (Ktor integration available) |
   | 2 | Log Monitor | Captures & displays app log messages (Kermit integration available) |
   | 3 | Preferences | Editable app settings panel, generated from `@SidekickPreferences` annotations |
   | 4 | Custom Screens | Wrap any Composable as a named debug screen in the overlay |

2. **Existing preferences class?** If you found one in Phase 1, ask:
   > "I found `[ClassName]` at `[path]`. Should I migrate it to the Sidekick
   > Preferences plugin? Note: composite types (e.g. two `Long` keys combined
   > into a date-range object) and nullable flows (`getOrNullFlow`) are not
   > supported by the annotation processor and must be kept manually."

   If the user says yes, collect the path for Phase 3.

---

## Phase 3 — Apply changes

### 3a. Repository wiring

Sidekick is consumed from Maven Local (for local dev) or Maven Central (once
published). Verify the consumer's root `settings.gradle.kts` has these
repositories registered. Add what's missing:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal() // remove once Sidekick is on Maven Central
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()  // remove once Sidekick is on Maven Central
    }
}
```

Sidekick is published to group `dev.parez.sidekick`. The current snapshot
version is `0.1.0-SNAPSHOT`. **Add to `gradle/libs.versions.toml`:**

```toml
[versions]
sidekick = "0.1.0-SNAPSHOT"

[libraries]
sidekick-runtime                = { module = "dev.parez.sidekick:runtime",                version.ref = "sidekick" }
sidekick-noop                   = { module = "dev.parez.sidekick:noop",                   version.ref = "sidekick" }
sidekick-preferences            = { module = "dev.parez.sidekick:preferences",            version.ref = "sidekick" }
sidekick-preferences-ksp        = { module = "dev.parez.sidekick:preferences-ksp",        version.ref = "sidekick" }
sidekick-network-monitor-plugin = { module = "dev.parez.sidekick:network-monitor-plugin", version.ref = "sidekick" }
sidekick-network-monitor-ktor   = { module = "dev.parez.sidekick:network-monitor-ktor",   version.ref = "sidekick" }
sidekick-log-monitor-plugin     = { module = "dev.parez.sidekick:log-monitor-plugin",     version.ref = "sidekick" }
sidekick-log-monitor-kermit     = { module = "dev.parez.sidekick:log-monitor-kermit",     version.ref = "sidekick" }
sidekick-custom-screens         = { module = "dev.parez.sidekick:custom-screens",         version.ref = "sidekick" }

[plugins]
sidekick-preferences = { id = "dev.parez.sidekick.preferences", version.ref = "sidekick" }
```

> **In-repo consumers only:** instead of the Maven coordinates above, you may
> use typesafe project accessors (`projects.core.runtime`, `projects.plugins.preferences.api`, etc.) — but only when the consuming module is part of the
> Sidekick mono-repo's `settings.gradle.kts`.

### 3b. Core dependency (debug/release swap)

The runtime overlay is in `dev.parez.sidekick:runtime`; the release stub is
`dev.parez.sidekick:noop`. Both expose an identical `Sidekick(plugins = …)`
symbol, so `commonMain` code compiles against either.

**On Android-only projects:**
```kotlin
dependencies {
    debugImplementation(libs.sidekick.runtime)
    releaseImplementation(libs.sidekick.noop)
}
```

**On KMP projects with multiple targets (composeApp-style):**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Pull the full overlay in commonMain so every non-Android target
            // (JVM, iOS, Wasm, JS) gets the real implementation. Android
            // overrides this in its own variant block below.
            implementation(libs.sidekick.runtime)
        }
    }
}

dependencies {
    // Android variant-specific: full overlay in debug, no-op stub in release.
    // The Sidekick() call site in commonMain compiles against the same
    // signature regardless of which artifact wins on Android.
    debugImplementation(libs.sidekick.runtime)
    releaseImplementation(libs.sidekick.noop)
}
```

If the project is **JVM-only / desktop-only** (no Android), use `jvmMain.dependencies` and accept that the runtime ships in release JVM builds too — there's no separate JVM release variant.

### 3c. Plugin dependencies

Add to `commonMain.dependencies {}`:

#### Network Monitor

```kotlin
implementation(libs.sidekick.network.monitor.plugin)
implementation(libs.sidekick.network.monitor.ktor)   // if you want the Ktor HttpClient interceptor
```

Also add per-platform Ktor client engines if not already present:
```kotlin
androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
jvmMain.dependencies     { implementation(libs.ktor.client.cio) }
jsMain.dependencies      { implementation(libs.ktor.client.js) }
wasmJsMain.dependencies  { implementation(libs.ktor.client.js) }
// iOS pulls ktor-client-darwin automatically via ktor-client-core's defaults.
```

#### Log Monitor

```kotlin
implementation(libs.sidekick.log.monitor.plugin)
implementation(libs.sidekick.log.monitor.kermit)   // if your app uses Kermit
implementation(libs.kermit)                         // if not already present
```

#### Preferences

```kotlin
implementation(libs.sidekick.preferences)
```

Then apply the Gradle plugin (see §3d).

#### Custom Screens

```kotlin
implementation(libs.sidekick.custom.screens)
```

#### Icons — required transitively

Sidekick plugins surface `Icons.Default.NetworkCheck`, `Icons.Default.Settings`,
`Icons.AutoMirrored.Default.List`, etc. through their public API. These icons
live in the **extended** Material icons artifact, not the foundation icons.
Add it to `commonMain`:

```kotlin
commonMain.dependencies {
    implementation(compose.materialIconsExtended)
}
```

Without this, every consumer-side `Icons.Default.…` import (and Sidekick's
own plugin grid rendering on some platforms) fails to compile.

### 3d. Preferences Gradle plugin (only if Preferences was selected)

The `dev.parez.sidekick.preferences` Gradle plugin handles all KSP wiring for
the preferences code generator — `commonMain.kotlin.srcDir`, processor dependency, configuration-cache-safe task ordering, and the stable-directory sync that prevents incremental-build "Unresolved reference" failures.

#### Apply the plugin

Add to `composeApp/build.gradle.kts` (or your app module's `build.gradle.kts`):

```kotlin
plugins {
    // … your existing plugins …
    alias(libs.plugins.ksp)                       // Required: the Sidekick plugin
                                                  // does not yet pull KSP transitively.
    alias(libs.plugins.sidekick.preferences)
}
```

#### JS / Wasm targets — extra srcDir wiring

The Gradle plugin currently only wires the `commonMain` generated-source
directory. For projects with JS and / or Wasm targets, also add:

```kotlin
kotlin {
    sourceSets {
        jsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin"))
        }
        wasmJsMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"))
        }
    }
}
```

> This step will become unnecessary once the Gradle plugin handles JS/Wasm directly. Track in the Sidekick repo's audit recommendations.

#### Adding the KSP processor dependency

The `dev.parez.sidekick.preferences` Gradle plugin applies KSP, wires the
generated-source directory, and orders the compile/KSP tasks — but it does
**not** auto-add the `preferences-ksp` artifact. Add it yourself on
`kspCommonMainMetadata`:

```kotlin
dependencies {
    add("kspCommonMainMetadata", "dev.parez.sidekick:preferences-ksp:<bom-version>")
}
```

For monorepo / composite-build setups, substitute a project dependency
(`projects.plugins.preferences.ksp`) without needing any opt-out flag.

### 3e. Preferences class migration

**Only if the user confirmed migration in Phase 2.**

Read the existing preferences class in full before making any changes.

#### Supported types

The KSP processor supports: `Boolean`, `String`, `Int`, `Long`, `Float`,
`Double`, and `enum class` types. For each supported property, create a
corresponding `@Preference`-annotated `var` property in the new class.

#### Unsupported — keep manually

Flag these and tell the user they must be kept outside the annotated class:

- Composite types (e.g. a `ForecastDateRange` built from two `Long` keys).
- Nullable flows (`getOrNullFlow`, `getBlockingOrNull`).
- Custom mapped types (e.g. `Flow<Instant>` derived from a `Long`).
- Enums whose values are stored via a manual `.value` string (not `.name`) —
  KSP uses `Enum.valueOf(name)` and would lose round-trip fidelity.

#### Transformation

Replace the existing class body with annotations. Example:

**Before:**
```kotlin
class AppPreferences(private val dataStore: DataStore<Preferences>) {
    val debugModeEnabledFlow: Flow<Boolean>
        get() = dataStore.getFlow(KEY_DEBUG_MODE, false)

    val debugModeEnabled: Boolean
        get() = dataStore.getBlocking(KEY_DEBUG_MODE, false)

    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.updateValue(KEY_DEBUG_MODE) { enabled }
    }
}
```

**After:**
```kotlin
@SidekickPreferences(title = "App")
class AppPreferences {
    @Preference(label = "Debug Mode")
    var debugMode: Boolean = false  // default value comes from this initializer
}
```

Add the imports:
```kotlin
import dev.parez.sidekick.preferences.Preference
import dev.parez.sidekick.preferences.SidekickPreferences
```

Remove: the `DataStore` constructor parameter, all `KEY_*` constants, all
hand-written `Flow`/blocking properties, and all suspend setters covered by
the annotation.

#### Updating call sites

After migration, search for all usages of the old class and update them:

| Old call | New call (on generated `*Accessor`) |
|----------|--------------------------------------|
| `debugModeEnabledFlow` | `debugMode` (`StateFlow<Boolean>`) |
| `debugModeEnabled` (blocking) | `debugMode.value` |
| `setDebugMode(x)` | `setDebugMode(x)` (same, still `suspend`) |
| `changeAppearance(x)` / `set*(x)` | `set*(x)` (generated name: `set` + capitalized property name) |
| `collectAsState()` on `*Flow` | `collectAsState()` on `StateFlow` property directly |

The generated accessor is accessed via `plugin.accessor` on the plugin instance:

```kotlin
val prefsPlugin = remember { AppPreferencesPlugin() }
val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
```

### 3f. Wire `Sidekick()` into the root composable

Find the root composable (identified in Phase 1). Read it, then edit it to:

1. **Instantiate selected plugins with `remember { … }`.** Plugins are
   stateful (in-memory stores, retention sweepers, Koin contexts) — they
   must be remembered or you'll spin up a fresh one each recomposition.

   ```kotlin
   // Preferences (KSP-generated)
   val prefsPlugin = remember { AppPreferencesPlugin() }

   // Network Monitor
   val networkPlugin = remember { NetworkMonitorPlugin() }

   // Log Monitor + Kermit bridge
   val logPlugin = remember {
       LogMonitorPlugin().also { plugin ->
           Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
       }
   }

   // Custom screen example
   val myDebugScreen = remember {
       CustomScreenPlugin(
           id = "com.myapp.debug",
           title = "Debug",
           icon = Icons.Default.BugReport,
       ) {
           // your Composable — host DI works here (Koin/Hilt/etc.)
       }
   }
   ```

   The correct imports for the bridge:
   ```kotlin
   import co.touchlab.kermit.Logger
   import co.touchlab.kermit.platformLogWriter
   import dev.parez.sidekick.logs.LogMonitorPlugin
   import dev.parez.sidekick.logs.kermit.LogMonitorLogWriter   // note .kermit segment
   import dev.parez.sidekick.network.NetworkMonitorPlugin
   ```

2. **Collect from the preferences accessor before `MaterialTheme`** if any
   preference drives theming:
   ```kotlin
   val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
   ```

3. **Build the plugin list with `remember(...)`** so its identity is stable
   across recompositions:
   ```kotlin
   val plugins = remember(prefsPlugin, networkPlugin, logPlugin, myDebugScreen) {
       listOf(prefsPlugin, networkPlugin, logPlugin, myDebugScreen)
   }
   ```

4. **Add a FAB + `AnimatedVisibility` + `Sidekick(...)` overlay.** The host
   owns visibility — `Sidekick()` only renders the panel content.

   ```kotlin
   MaterialTheme(/* host's existing color scheme */) {
       var sidekickVisible by remember { mutableStateOf(false) }

       Scaffold(
           floatingActionButton = {
               if (!sidekickVisible) {
                   FloatingActionButton(onClick = { sidekickVisible = true }) {
                       Icon(Icons.Default.BugReport, contentDescription = "Open Sidekick")
                   }
               }
           },
       ) { padding ->
           Box(Modifier.fillMaxSize().padding(padding)) {
               // ── existing app content ──
               YourExistingScreen(...)

               AnimatedVisibility(visible = sidekickVisible) {
                   Sidekick(
                       plugins = plugins,
                       useSidekickTheme = false, // inherit the host's MaterialTheme
                       actions = {
                           IconButton(onClick = { sidekickVisible = false }) {
                               Icon(Icons.Default.Close, contentDescription = "Close Sidekick")
                           }
                       },
                   )
               }
           }
       }
   }
   ```

Do **not** introduce a new outer wrapper composable — keep `Sidekick()`
inside the host's `MaterialTheme` so theming is inherited automatically when
`useSidekickTheme = false`. There is no `SidekickShell`; do not invent one.

### 3g. Install `NetworkMonitorKtor` on the HttpClient (Network Monitor + Ktor only)

The Ktor integration is a `ClientPlugin` you install on the `HttpClient`
instance the consumer is already using:

```kotlin
import dev.parez.sidekick.network.ktor.NetworkMonitorKtor

val httpClient = HttpClient {
    install(NetworkMonitorKtor)
    // … your existing config …
}
```

The plugin reads the default `NetworkMonitorStore` from the plugin's isolated
Koin context — no manual store wiring needed in the common case.

---

## Phase 4 — Report

Tell the user:

1. Which files were modified and what was changed in each.
2. Any preference properties that were **not migrated** (unsupported types) and why.
3. Any manual steps remaining:
   - Installing `NetworkMonitorKtor` on the `HttpClient` (if network-monitor selected and the Ktor client lives outside the file you edited).
   - Wiring the Kermit log writer if they use a non-Kermit logger.
   - Handling any unsupported preference properties kept outside the class.
   - Syncing Gradle and verifying the build:
     ```bash
     ./gradlew :composeApp:assembleDebug
     ./gradlew :composeApp:jvmJar
     ./gradlew :composeApp:wasmJsBrowserProductionWebpack
     ./gradlew :composeApp:compileKotlinIosSimulatorArm64
     ```

---

## Rules

- Read every file before editing it.
- Do **not** remove KSP dependencies that were already present for other
  purposes (e.g. Room uses KSP too).
- Do **not** touch release build types or signing config.
- Do **not** add `kspAndroid` / `kspJvm` / `kspJs` / `kspWasmJs` for the
  Preferences processor — only `kspCommonMainMetadata` is correct. The
  Gradle plugin handles this; do not bypass it.
- Do **not** overwrite existing plugin registrations in the existing plugin
  list passed to `Sidekick(...)`.
- Do **not** invent or reference `SidekickShell` — the real API is
  `Sidekick(plugins = …)` and the host owns the FAB / visibility.
- Do **not** migrate enum types whose values are stored via a manual `.value`
  string property (not `.name`) — these require a custom mapping that KSP
  cannot infer.
- If `@SidekickPreferences` is already present on the class, skip migration
  and tell the user KSP is already set up.
- Prefer Maven coordinates (`libs.sidekick.*`) over `projects.*` accessors
  unless the consuming module is inside the Sidekick mono-repo.
