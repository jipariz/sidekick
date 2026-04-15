---
name: setup-sidekick
description: >
  Interactive setup wizard for adding Sidekick to a consumer app. Handles
  fresh installation (core dependencies, SidekickShell wiring), plugin
  selection (network-monitor, log-monitor, preferences, custom-screens),
  and migration of an existing DataStore-based preferences class to the
  Preferences plugin with KSP code generation. Trigger with: "set up
  Sidekick", "add Sidekick", "install Sidekick", "migrate preferences to
  Sidekick", or just "/setup-sidekick".
argument-hint: "[path/to/app/build.gradle.kts]"
allowed-tools: Read Write Edit Bash Glob Grep AskUserQuestion
---

# Sidekick Setup & Preferences Migration Wizard

You are setting up Sidekick in a consumer project. Work through the phases
below in order. Adapt to what you find — skip steps that are already done.

---

## Phase 1 — Discover

Find the app module's `build.gradle.kts`. If `$ARGUMENTS` is a path, use it.
Otherwise, search for `build.gradle.kts` files that look like app modules
(containing `com.android.application` or a `compose.desktop` block).

Read the file and extract:

- **Targets declared** — `androidTarget`, `jvm()`, `js`, `wasmJs`, iOS
- **Existing Sidekick dependencies** — any `projects.core.*` or `projects.plugins.*`
- **KSP plugin** — is `alias(libs.plugins.ksp)` already applied?
- **Root composable file** — search for a `@Composable fun` that contains
  `MaterialTheme` or is the app entry point (commonly `App.kt`, `DemoApp.kt`,
  `MainActivity.kt`, or similar). Read it to understand the current shape.

Also search `commonMain` sources for a class that:
- Has `DataStore<Preferences>` as a constructor parameter, **or**
- Uses `dataStore.getFlow(...)`, `dataStore.getBlocking(...)`, `dataStore.updateValue(...)`, **or**
- Is already annotated with `@SidekickPreferences`

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

### 3a. Core dependency

If `core:runtime` / `core:noop` are not already present:

```kotlin
// In the top-level dependencies block (Android builds)
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}
```

If the project has a **JVM target** and no Android target, add to `jvmMain.dependencies` instead:

```kotlin
jvmMain.dependencies {
    implementation(projects.core.runtime)
}
```

If both Android and JVM targets exist, add both blocks.

### 3b. Plugin dependencies

Add to `commonMain.dependencies {}` inside the `kotlin {}` block:

#### Network Monitor

```kotlin
implementation(projects.plugins.networkMonitor.plugin)
```

If the user wants Ktor integration:

```kotlin
implementation(projects.plugins.networkMonitor.ktor)
```

#### Log Monitor

```kotlin
implementation(projects.plugins.logMonitor.plugin)
```

If the user wants Kermit integration:

```kotlin
implementation(projects.plugins.logMonitor.kermit)
```

#### Preferences

```kotlin
implementation(projects.plugins.preferences.api)
```

Then add the KSP processor (see §3c).

#### Custom Screens

```kotlin
implementation(projects.plugins.customScreens.api)
```

### 3c. KSP setup (Preferences plugin only)

**Only if Preferences was selected and KSP is not already configured.**

1. Add the KSP plugin to the `plugins {}` block:

```kotlin
alias(libs.plugins.ksp)
```

2. Wire generated sources in `commonMain`:

```kotlin
commonMain {
    kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
}
```

3. Register the processor in the `dependencies {}` block:

```kotlin
add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
```

4. Add task wiring **after** the `kotlin {}` block. Use `configureEach` (not
   `matching + configureEach`) so it stays compatible with the configuration cache:

```kotlin
tasks.configureEach {
    if (name != "kspCommonMainKotlinMetadata" &&
        ((name.startsWith("compile") && name.contains("Kotlin")) || name.startsWith("ksp"))
    ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
tasks.configureEach {
    if (name == "kspCommonMainKotlinMetadata") {
        outputs.cacheIf { false }
        val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
        outputs.upToDateWhen { outDir.get().asFile.exists() }
    }
}
```

If the project has **JS or WasmJS targets**, also add their generated source
directories:

```kotlin
jsMain {
    kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin"))
}
wasmJsMain {
    kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"))
}
```

### 3d. Preferences class migration

**Only if the user confirmed migration in Phase 2.**

Read the existing preferences class in full before making any changes.

#### Supported types

The KSP processor supports: `Boolean`, `String`, `Int`, `Long`, `Float`,
`Double`, and `enum class` types. For each supported property, create a
corresponding `@Preference`-annotated `var` property in the new class.

#### Unsupported — keep manually

Flag these and tell the user they must be kept outside the annotated class:

- Composite types (e.g. a `ForecastDateRange` built from two `Long` keys)
- Nullable flows (`getOrNullFlow`, `getBlockingOrNull`)
- Custom mapped types (e.g. `Flow<Instant>` derived from a `Long`)

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
    @Preference(label = "Debug Mode", defaultValue = "false")
    var debugMode: Boolean = false
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

### 3e. Wire SidekickShell

Find the root composable (identified in Phase 1). Read it, then edit it to:

1. Instantiate selected plugins with `remember { ... }`:

```kotlin
// Network Monitor
val networkPlugin = remember { NetworkMonitorPlugin() }

// Network Monitor with Ktor — also install on your HttpClient:
// httpClient.install(NetworkMonitorKtor)

// Log Monitor
val logPlugin = remember {
    LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_HOUR).also { plugin ->
        // Kermit bridge (if selected):
        Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
    }
}

// Preferences (KSP-generated)
val prefsPlugin = remember { AppPreferencesPlugin() }

// Custom Screen example
val myDebugScreen = remember {
    CustomScreenPlugin(
        id = "com.myapp.debug",
        title = "Debug",
        icon = Icons.Default.BugReport,
    ) {
        // your Composable — DI works here
    }
}
```

2. Collect from the preferences accessor where needed (observe before `MaterialTheme`):

```kotlin
val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
```

3. Wrap the app content with `SidekickShell`:

```kotlin
val plugins = remember(...) { listOf(/* selected plugins */) }

SidekickShell(plugins = plugins) {
    // existing app content
}
```

Do **not** move the existing `MaterialTheme` call — keep `SidekickShell`
inside it so the overlay inherits the app's color scheme automatically.

---

## Phase 4 — Report

Tell the user:

1. Which files were modified and what was changed in each.
2. Any properties that were **not migrated** (unsupported types) and why.
3. Any manual steps remaining:
   - Installing `NetworkMonitorKtor` on the `HttpClient` (if network-monitor selected).
   - Wiring the Kermit log writer if they use a non-Kermit logger.
   - Handling any unsupported preference properties kept outside the class.
   - Syncing Gradle and verifying the build (`./gradlew :app:assembleDebug` or `./gradlew :app:run`).

---

## Rules

- Read every file before editing it.
- Do **not** remove DataStore or KSP dependencies that were already present
  for other purposes (e.g. Room uses KSP too).
- Do **not** touch release build types or signing config.
- Do **not** add `kspAndroid` / `kspJvm` / `kspJs` / `kspWasmJs` for the
  Preferences processor — only `kspCommonMainMetadata` is correct.
- Do **not** overwrite existing plugin registrations in `SidekickShell`.
- Do **not** migrate enum types whose values are stored via a manual `.value`
  string property (not `.name`) — these require a custom mapping that KSP
  cannot infer.
- If `@SidekickPreferences` is already present on the class, skip migration
  and tell the user KSP is already set up.
