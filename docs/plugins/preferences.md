# Preferences

Expose typed app settings inside the Sidekick panel — flip feature flags, change an API URL, toggle dark mode, all without rebuilding. Recommended setup uses a KSP annotation processor to generate the DataStore boilerplate. Already have a hand-written DataStore class? Bridge it in a few lines without code generation.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)*

<sub>* On Wasm the preferences plugin falls back to an in-memory store, since DataStore has no Wasm driver — values do not persist across page reloads.</sub>

## Features

- **Type-safe accessors** — `StateFlow<T>` for reading, `suspend fun setX(value: T)` for writing.
- **Minimal annotations** — a single `@SidekickPreferences` on the class is enough; defaults come from each property's Kotlin initializer.
- **KSP code generation** — annotate a plain class, get the DataStore wiring + a ready-to-use `SidekickPlugin` for free.
- **Six primitive types** — `Boolean`, `String`, `Int`, `Long`, `Float`, `Double`.
- **Enum chip picker** — `EnumPref` renders a chip row for any string-backed enum.
- **DataStore-backed persistence** — on Android, iOS, Desktop, and JS.
- **Adaptive grid UI** — 1 / 2 / 3 columns by width.
- **Migration-friendly** — bridge an existing DataStore class without touching its data.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:preferences:api` | `@SidekickPreferences` / `@Preference` annotations, `PreferencesPlugin`, DataStore-backed `PreferenceStore`. |
| `:plugins:preferences:ksp` | JVM-only KSP processor that generates type-safe accessors and `*Plugin` classes from annotations. |
| `:plugins:preferences:gradle-plugin` | Convention Gradle plugin that wires the KSP processor and generated-sources dir. |

## Setup

### 1. Add dependencies

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:2026.05.16"))
            implementation("dev.parez.sidekick:preferences")
        }
    }
}

dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.05.16"))
    debugImplementation("dev.parez.sidekick:runtime")   // version from BOM
    releaseImplementation("dev.parez.sidekick:noop")    // version from BOM
}
```

### 2. Configure KSP

#### Recommended: apply the Sidekick Preferences Gradle plugin

The `dev.parez.sidekick.preferences` Gradle plugin bundles the KSP plugin application, the generated-sources directory registration, and the task-dependency wiring. Apply it, then add the processor to `kspCommonMainMetadata`:

```kotlin
plugins {
    id("dev.parez.sidekick.preferences") version "2026.05.16"
}

dependencies {
    add("kspCommonMainMetadata", "dev.parez.sidekick:preferences-ksp:0.1.0")
}
```

(Sidekick deliberately does not auto-add the processor — keeping that line in your build script makes it obvious which KSP processors are running, and lets monorepo setups substitute a project dependency without an opt-out flag.)

#### Manual (no Gradle plugin)

If you'd rather not apply the `dev.parez.sidekick.preferences` Gradle plugin — for example to keep your KSP wiring identical to other code generators in the same module — you can do the same wiring directly:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        }
        // KSP emits per-target outputs for JS / WasmJS; register them too if you have those targets.
        jsMain { kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin")) }
        wasmJsMain { kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin")) }
    }
}

dependencies {
    add("kspCommonMainMetadata", "dev.parez.sidekick:preferences-ksp:0.1.0")
}

// All compile and KSP tasks must wait for the common-metadata KSP pass.
tasks.configureEach {
    if (name != "kspCommonMainKotlinMetadata" &&
        ((name.startsWith("compile") && name.contains("Kotlin")) || name.startsWith("ksp"))
    ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
```

For **Android-only modules** (not Kotlin Multiplatform) the KSP plumbing collapses to two lines — KSP's standard `ksp(...)` configuration auto-registers the generated source dir:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation("dev.parez.sidekick:preferences:0.1.0")
    ksp("dev.parez.sidekick:preferences-ksp:0.1.0")
}
```

!!! tip "Automated setup"
    The [`/setup-sidekick`](../claude-code-skills.md#setup-sidekick) Claude Code skill wires all of this for you.

### 3. Wire into Sidekick

```kotlin
val prefsPlugin = remember { AppPreferencesPlugin() }

Sidekick(
    plugins = listOf(prefsPlugin),
    actions = {
        IconButton(onClick = { sidekickVisible = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    },
)
```

## Defining Preferences

Annotate a class with `@SidekickPreferences`. **Every property of the class becomes a preference**, and the default value comes from the property's Kotlin initializer — no extra annotation needed for the common case:

```kotlin
@SidekickPreferences  // title and storeName auto-derived from the class name
class AppPreferences {
    var darkMode: Boolean = false
    var apiUrl: String = "https://api.example.com"
    var timeout: Int = 30
    var colorTheme: ColorTheme = ColorTheme.DEFAULT   // enum entry → EnumPref chip row
}

enum class ColorTheme { DEFAULT, FIRE, WATER, GRASS }
```

UI labels are humanised from each property name (`darkMode` → "Dark Mode"). To override a label, a description, or both — use `@Preference`:

```kotlin
@SidekickPreferences(
    title = "App Settings",    // panel heading; also drives the default DataStore file name
    storeName = "",            // optional — defaults to title.lowercase().replace(" ", "_")
)
class AppPreferences {
    var darkMode: Boolean = false

    @Preference(label = "API endpoint")
    var apiUrl: String = "https://api.example.com"

    @Preference(
        label = "Feature flag",
        description = "Enables the experimental new checkout flow.",
    )
    var newCheckout: Boolean = false

    @IgnorePreference   // skipped by the processor entirely
    var internalCache: String = ""
}
```

Supported property types: `Boolean`, `String`, `Int`, `Long`, `Float`, `Double`, and any Kotlin `enum` (auto-detected — KSP emits an `EnumPref`). The processor reads the property's Kotlin initializer (`= ...`) and emits it as the preference's default. Properties without an initializer fall back to the type-zero value (`false`, `0`, `""`, or the first enum entry).

## Generated Code

KSP generates two classes from every `@SidekickPreferences` class:

**`<Class>Accessor`** — reactive read + write API:

```kotlin
// Reading (collect in Compose with collectAsState())
val darkMode: StateFlow<Boolean>
val apiUrl: StateFlow<String>
val timeout: StateFlow<Int>
val colorTheme: StateFlow<ColorTheme>

// Writing
suspend fun setDarkMode(value: Boolean)
suspend fun setApiUrl(value: String)
suspend fun setTimeout(value: Int)
suspend fun setColorTheme(value: ColorTheme)
```

**`<Class>Plugin`** — the `SidekickPlugin` implementation. Pass it straight to `Sidekick`. The accessor is exposed as `plugin.accessor`.

## Usage

```kotlin
@Composable
fun App() {
    val prefsPlugin = remember { AppPreferencesPlugin() }
    val darkMode by prefsPlugin.accessor.darkMode.collectAsState()
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        Button(onClick = { scope.launch { prefsPlugin.accessor.setDarkMode(!darkMode) } }) {
            Text("Toggle Dark Mode")
        }
        // ...
        Sidekick(plugins = listOf(prefsPlugin), actions = { /* close button */ })
    }
}
```

## UI

The panel adapts to width:

| Width | Layout |
|---|---|
| < 600 dp | Single-column list with inline editors. |
| 600 – 840 dp | 2-column card grid. |
| ≥ 840 dp | 3-column card grid. |

Each card shows the preference type badge (`BOOL` / `STR` / `INT` / `ENUM` / …), label, and an inline editor:

- **Boolean** — toggle switch.
- **String / Int / Long / Float / Double** — `OutlinedTextField` with a Save button that enables only when the value changes.
- **Enum** — selectable chip row.

## Advanced

### Migrating from an existing DataStore class

If you already have a hand-written `DataStore<Preferences>` class, choose either:

- **Option A — Replace with annotations** *(recommended)*. Delete your store, add `@SidekickPreferences`/`@Preference` to a plain class, let KSP regenerate accessors. Preserve your existing DataStore file by setting `storeName` to match the old file name.
- **Option B — Bridge in place**. Wrap your existing store with a `PreferencesPlugin` subclass. No KSP setup required.

#### Option A — replace with KSP annotations

1. Configure KSP per the [Setup](#2-configure-ksp) section above.
2. Delete or rename your existing `AppSettingsStore` class.
3. Create the annotated replacement:

    ```kotlin
    @SidekickPreferences(title = "App Settings", storeName = "app_preferences")
    class AppPreferences {
        var darkMode: Boolean = false
        var apiUrl: String = "https://api.example.com"

        @Preference(label = "Request Timeout (s)")
        var timeout: Int = 30
    }
    ```

    !!! tip "Preserving existing DataStore data"
        The generated accessor derives the DataStore file name from `title` (lowercased, spaces → underscores). Pass `storeName` explicitly to match your existing file (`app_preferences.preferences_pb`) so stored values aren't lost. The DataStore key for each property is the property name verbatim (`darkMode`, `apiUrl`, …) — if your existing keys differ, rename the properties to match.

4. Build the project. KSP generates `AppPreferencesAccessor` and `AppPreferencesPlugin`.
5. Replace usages:

    | Before | After |
    |---|---|
    | `store.darkMode.collectAsState()` | `prefsPlugin.accessor.darkMode.collectAsState()` |
    | `store.setDarkMode(true)` | `prefsPlugin.accessor.setDarkMode(true)` |
    | `store.apiUrl.value` | `prefsPlugin.accessor.apiUrl.value` |

6. Add the plugin to Sidekick.

#### Option B — bridge in place

Keep your DataStore class unchanged. Subclass `PreferencesPlugin` and wire it explicitly — no KSP needed:

```kotlin
class AppSettingsPlugin(
    private val store: AppSettingsStore,
) : PreferencesPlugin(
    pluginTitle = "App Settings",
    definitions = listOf(
        BooleanPref(key = "dark_mode",       label = "Dark Mode",            description = "", defaultValue = false),
        StringPref( key = "api_url",         label = "API URL",              description = "", defaultValue = "https://api.example.com"),
        IntPref(    key = "request_timeout", label = "Request Timeout (s)",  description = "", defaultValue = 30),
    ),
    valueFlows = mapOf(
        "dark_mode"       to store.darkMode,
        "api_url"         to store.apiUrl,
        "request_timeout" to store.timeout,
    ),
    onSet = { key, value ->
        when (key) {
            "dark_mode"       -> store.setDarkMode(value as Boolean)
            "api_url"         -> store.setApiUrl(value as String)
            "request_timeout" -> store.setTimeout(value as Int)
        }
    },
)
```

The keys across `definitions`, `valueFlows`, and `onSet` must match.

!!! info "`valueFlows` expects `StateFlow<Any>`, not cold `Flow`"
    If your existing store exposes cold `Flow<T>` (typical for DataStore-backed classes), wrap with `.stateIn(scope, …)` before passing it in:
    ```kotlin
    private fun <T : Any> Flow<T>.toAnyStateFlow(scope: CoroutineScope, initial: T): StateFlow<Any> =
        map<T, Any> { it }.stateIn(scope, SharingStarted.Eagerly, initial)
    ```
    Then `valueFlows = mapOf("dark_mode" to store.darkModeFlow.toAnyStateFlow(scope, false), …)`.

### Manual setup without KSP

If you're starting from scratch and prefer not to use code generation, `PreferencesPlugin` accepts the definitions and flows directly (same shape as Option B above):

```kotlin
class MyPreferencesPlugin : PreferencesPlugin(
    pluginTitle = "App Settings",
    definitions = listOf(
        BooleanPref(key = "dark_mode", label = "Dark Mode", description = "", defaultValue = false),
        StringPref(key = "api_url",    label = "API URL",   description = "", defaultValue = "https://example.com"),
        EnumPref(
            key = "color_theme",
            label = "Color Theme",
            description = "",
            defaultValue = "DEFAULT",
            options = listOf("DEFAULT", "FIRE", "WATER", "GRASS"),
        ),
    ),
    valueFlows = mapOf(
        "dark_mode"   to myStore.darkMode,
        "api_url"     to myStore.apiUrl,
        "color_theme" to myStore.colorTheme,
    ),
    onSet = { key, value ->
        when (key) {
            "dark_mode"   -> myStore.setDarkMode(value as Boolean)
            "api_url"     -> myStore.setApiUrl(value as String)
            "color_theme" -> myStore.setColorTheme(value as String)
        }
    },
)
```

## See also

- [Custom Screens](custom-screens.md)
- [Custom plugin](custom-plugin.md)
