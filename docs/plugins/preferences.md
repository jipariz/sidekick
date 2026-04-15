# Preferences

The Preferences plugin exposes typed app settings in the Sidekick panel. Use the KSP annotation processor to generate the required boilerplate automatically.

## Defining Preferences

Annotate a class with `@SidekickPreferences` and its properties with `@Preference`:

```kotlin
@SidekickPreferences(title = "App Settings")
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

## Generated Code

KSP generates two classes from the annotated class:

**`AppPreferencesAccessor`** — reactive state for reading and writing preferences:

```kotlin
// Reading (StateFlow — use collectAsState() in Compose)
val darkMode: StateFlow<Boolean>
val apiUrl: StateFlow<String>
val timeout: StateFlow<Int>

// Writing
suspend fun setDarkMode(value: Boolean)
suspend fun setApiUrl(value: String)
suspend fun setTimeout(value: Int)
```

**`AppPreferencesPlugin`** — the `SidekickPlugin` implementation, ready to pass to `SidekickShell`.

## Usage

```kotlin
@Composable
fun App() {
    val prefsPlugin = remember { AppPreferencesPlugin() }
    val darkMode by prefsPlugin.accessor.darkMode.collectAsState()

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        SidekickShell(plugins = listOf(prefsPlugin)) {
            MyAppContent()
        }
    }
}
```

## UI

The Preferences panel adapts to screen width:

| Width | Layout |
|-------|--------|
| < 600 dp | Single-column list with inline editors |
| 600–840 dp | 2-column card grid |
| ≥ 840 dp | 3-column card grid |

Each card shows the preference type badge (`BOOL` / `STR` / `INT` / etc.), label, and an inline editor. Boolean cards have a toggle switch; string and number cards have an `OutlinedTextField` with a Save button that enables only when the value has changed.

## Manual Setup (without KSP)

You can extend `PreferencesPlugin` directly if you prefer not to use code generation:

```kotlin
class MyPreferencesPlugin : PreferencesPlugin(
    pluginTitle = "App Settings",
    definitions = listOf(
        BooleanPref(key = "dark_mode", label = "Dark Mode", description = "", default = false),
        StringPref(key = "api_url", label = "API URL", description = "", default = "https://example.com"),
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

!!! tip "Migrating from DataStore"
    The [`/setup-sidekick`](../claude-code-skills.md#setup-sidekick) skill can automatically migrate an existing hand-written DataStore preferences class to the `@SidekickPreferences` annotation approach.
