# Preferences

The Preferences plugin exposes typed app settings in the Sidekick panel. The recommended approach uses a KSP annotation processor to generate the required boilerplate automatically. If you already have a DataStore-based preferences class, see [Migrating from DataStore](#migrating-from-datastore).

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

### 2. Configure KSP (for code generation)

Apply the KSP plugin and register the annotation processor:

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

// tasks.matching is incompatible with Gradle's configuration cache — use configureEach + if instead
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

---

## Defining Preferences

Annotate a class with `@SidekickPreferences` and its properties with `@Preference`:

```kotlin
@SidekickPreferences(
    title = "App Settings",   // panel heading
    storeName = "",           // DataStore file name; defaults to title lowercased with spaces → underscores
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
        description = "Enables the experimental new checkout flow",
        defaultValue = "false",
    )
    var newCheckout: Boolean = false
}
```

Supported property types: `Boolean`, `String`, `Int`, `Long`, `Float`, `Double`.

---

## Generated Code

KSP generates two classes from the annotated class:

**`AppPreferencesAccessor`** — reactive state for reading and writing preferences:

```kotlin
// Reading (collect in Compose with collectAsState())
val darkMode: StateFlow<Boolean>
val apiUrl: StateFlow<String>
val timeout: StateFlow<Int>

// Writing
suspend fun setDarkMode(value: Boolean)
suspend fun setApiUrl(value: String)
suspend fun setTimeout(value: Int)
```

**`AppPreferencesPlugin`** — the `SidekickPlugin` implementation, ready to pass to `Sidekick`.

---

## Usage

```kotlin
@Composable
fun App() {
    val prefsPlugin = remember { AppPreferencesPlugin() }
    val darkMode by prefsPlugin.accessor.darkMode.collectAsState()

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        // ...
        Sidekick(plugins = listOf(prefsPlugin), onClose = { ... })
    }
}
```

---

## UI

The Preferences panel adapts to screen width:

| Width | Layout |
|---|---|
| < 600 dp | Single-column list with inline editors |
| 600–840 dp | 2-column card grid |
| ≥ 840 dp | 3-column card grid |

Each card shows the preference type badge (`BOOL` / `STR` / `INT` / etc.), label, and an inline editor. Boolean cards have a toggle switch; string and number cards have an `OutlinedTextField` with a Save button that enables only when the value has changed.

---

## Migrating from DataStore

If you already have a hand-written DataStore preferences class, you can either replace it with the annotation-based approach (Option A) or keep it and bridge it to Sidekick (Option B). Both options work without any AI assistance.

### What you likely have

```kotlin
// Existing: AppSettingsStore.kt
class AppSettingsStore(
    private val scope: CoroutineScope,
    private val dataStore: DataStore<Preferences>,
) {
    val darkMode: StateFlow<Boolean> = dataStore.data
        .map { it[booleanPreferencesKey("dark_mode")] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val apiUrl: StateFlow<String> = dataStore.data
        .map { it[stringPreferencesKey("api_url")] ?: "https://api.example.com" }
        .stateIn(scope, SharingStarted.Eagerly, "https://api.example.com")

    val timeout: StateFlow<Int> = dataStore.data
        .map { it[intPreferencesKey("request_timeout")] ?: 30 }
        .stateIn(scope, SharingStarted.Eagerly, 30)

    suspend fun setDarkMode(v: Boolean) = dataStore.edit { it[booleanPreferencesKey("dark_mode")] = v }
    suspend fun setApiUrl(v: String)    = dataStore.edit { it[stringPreferencesKey("api_url")] = v }
    suspend fun setTimeout(v: Int)      = dataStore.edit { it[intPreferencesKey("request_timeout")] = v }
}
```

---

### Option A — Replace with KSP annotations (recommended)

Complete setup is described in [Setup](#setup) above. The key change: you replace your hand-written DataStore class with an annotated plain class, and let Sidekick's KSP processor generate the DataStore wiring for you.

**Step 1.** Configure KSP as described above.

**Step 2.** Delete (or rename) your existing `AppSettingsStore` class.

**Step 3.** Create the annotated replacement:

```kotlin
@SidekickPreferences(title = "App Settings")
class AppPreferences {
    @Preference(label = "Dark Mode", defaultValue = "false")
    var darkMode: Boolean = false

    @Preference(label = "API URL", defaultValue = "https://api.example.com")
    var apiUrl: String = ""

    @Preference(label = "Request Timeout (s)", defaultValue = "30")
    var timeout: Int = 0
}
```

**Step 4.** Build the project. KSP generates `AppPreferencesAccessor` and `AppPreferencesPlugin`.

**Step 5.** Replace all usages of your old store with the generated accessor:

| Before | After |
|--------|-------|
| `store.darkMode.collectAsState()` | `prefsPlugin.accessor.darkMode.collectAsState()` |
| `store.setDarkMode(true)` | `prefsPlugin.accessor.setDarkMode(true)` |
| `store.apiUrl.value` | `prefsPlugin.accessor.apiUrl.value` |

**Step 6.** Add the plugin to Sidekick:

```kotlin
val prefsPlugin = remember { AppPreferencesPlugin() }

Sidekick(plugins = listOf(prefsPlugin), onClose = { ... })
```

!!! tip "Preserving existing DataStore data"
    The generated accessor derives the DataStore file name from `title` (lowercased, spaces → underscores). If your existing DataStore used a different file name, pass `storeName` explicitly to match it:

    ```kotlin
    @SidekickPreferences(title = "App Settings", storeName = "app_preferences")
    class AppPreferences { ... }
    ```

    This tells the generated accessor to open `app_preferences.preferences_pb` — the exact same file your old store was writing to. Without this, stored values are lost on the next app launch.

---

### Option B — Bridge your existing store (no code generation)

Keep your DataStore class unchanged and create a `PreferencesPlugin` subclass that wires it to Sidekick's UI. No KSP setup needed.

**Step 1.** Create the plugin class:

```kotlin
class AppSettingsPlugin(
    private val store: AppSettingsStore,
) : PreferencesPlugin(
    pluginTitle = "App Settings",
    definitions = listOf(
        BooleanPref(key = "dark_mode",       label = "Dark Mode",            description = "", default = false),
        StringPref( key = "api_url",         label = "API URL",              description = "", default = "https://api.example.com"),
        IntPref(    key = "request_timeout", label = "Request Timeout (s)",  description = "", default = 30),
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

The keys in `definitions`, `valueFlows`, and `onSet` must all match.

**Step 2.** Instantiate and pass to Sidekick:

```kotlin
val store = AppSettingsStore(coroutineScope, dataStore)
val prefsPlugin = AppSettingsPlugin(store)

Sidekick(plugins = listOf(prefsPlugin), onClose = { ... })
```

Your existing DataStore file, keys, and all call sites remain untouched.

---

## Manual Setup (without KSP)

If you are starting from scratch and prefer not to use code generation, `PreferencesPlugin` accepts the definitions and flows directly — same as Option B above but without an existing store to bridge:

```kotlin
class MyPreferencesPlugin : PreferencesPlugin(
    pluginTitle = "App Settings",
    definitions = listOf(
        BooleanPref(key = "dark_mode", label = "Dark Mode", description = "", default = false),
        StringPref(key = "api_url",    label = "API URL",   description = "", default = "https://example.com"),
    ),
    valueFlows = mapOf(
        "dark_mode" to myStore.darkMode,
        "api_url"   to myStore.apiUrl,
    ),
    onSet = { key, value ->
        when (key) {
            "dark_mode" -> myStore.setDarkMode(value as Boolean)
            "api_url"   -> myStore.setApiUrl(value as String)
        }
    },
)
```
