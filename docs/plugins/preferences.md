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
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.plugins.preferences.api)
        }
    }
}
```

### 2. Configure KSP

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
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
}

// All compile and KSP tasks must wait for the common-metadata KSP pass.
tasks.configureEach {
    if (name != "kspCommonMainKotlinMetadata" &&
        ((name.startsWith("compile") && name.contains("Kotlin")) || name.startsWith("ksp"))
    ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

// Disable build caching for the KSP task (source-dir registration is unreliable in cache).
tasks.configureEach {
    if (name == "kspCommonMainKotlinMetadata") {
        outputs.cacheIf { false }
    }
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

Annotate a class with `@SidekickPreferences` and its properties with `@Preference`:

```kotlin
@SidekickPreferences(
    title = "App Settings",    // panel heading; also drives the default DataStore file name
    storeName = "",            // optional — defaults to title.lowercase().replace(" ", "_")
)
class AppPreferences {
    @Preference(label = "Dark Mode", defaultValue = "false")
    var darkMode: Boolean = false

    @Preference(label = "API URL", defaultValue = "https://api.example.com")
    var apiUrl: String = ""

    @Preference(label = "Request Timeout (s)", defaultValue = "30")
    var timeout: Int = 0

    @Preference(
        label = "Feature Flag",
        description = "Enables the experimental new checkout flow.",
        defaultValue = "false",
    )
    var newCheckout: Boolean = false

    // Enum prefs render as a chip row. Stored as the enum entry name.
    @Preference(label = "Color Theme", defaultValue = "DEFAULT")
    var colorTheme: ColorTheme = ColorTheme.DEFAULT
}

enum class ColorTheme { DEFAULT, FIRE, WATER, GRASS }
```

Supported property types: `Boolean`, `String`, `Int`, `Long`, `Float`, `Double`, and any Kotlin `enum` (auto-detected — KSP emits an `EnumPref`).

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
        @Preference(label = "Dark Mode", defaultValue = "false")
        var darkMode: Boolean = false

        @Preference(label = "API URL", defaultValue = "https://api.example.com")
        var apiUrl: String = ""

        @Preference(label = "Request Timeout (s)", defaultValue = "30")
        var timeout: Int = 0
    }
    ```

    !!! tip "Preserving existing DataStore data"
        The generated accessor derives the DataStore file name from `title` (lowercased, spaces → underscores). Pass `storeName` explicitly to match your existing file (`app_preferences.preferences_pb`) so stored values aren't lost.

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
