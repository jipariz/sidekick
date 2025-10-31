# Sidekick

A Kotlin Multiplatform debug overlay SDK for Android, iOS, Desktop (JVM), and Web (JS/Wasm). Sidekick adds a floating bug-report button to your app that opens a panel with pluggable debug tools — network inspector, preferences editor, and more.

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Plugins](#plugins)
  - [Network Monitor](#network-monitor)
  - [Preferences](#preferences)
- [Custom Theming](#custom-theming)
- [Creating a Custom Plugin](#creating-a-custom-plugin)
- [Release Builds](#release-builds)

---

## Installation

Sidekick is a multi-module library. Add the modules you need as dependencies in your app's `build.gradle.kts`.

### Core

Every app needs the core runtime (debug builds) and the no-op stub (release builds):

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation(projects.core.debug)
    releaseImplementation(projects.core.noop)
}
```

> **`core:noop`** compiles to a single `content()` call with zero overhead — Sidekick is completely absent from release builds.

For Desktop (JVM), add both explicitly since Gradle's `debugImplementation` is Android-only:

```kotlin
commonMain.dependencies {
    // ...
}
jvmMain.dependencies {
    implementation(projects.core.debug)
}
```

### Plugins

Add the plugins you want to use:

```kotlin
commonMain.dependencies {
    implementation(projects.plugins.preferences.api)
    implementation(projects.plugins.networkMonitor.api)
    implementation(projects.plugins.networkMonitor.ktor) // Ktor integration
}
```

### KSP (for the Preferences code generator)

The Preferences plugin includes a KSP processor that generates boilerplate from annotations. Apply the KSP plugin and register the processor:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            // point to the KSP output directory
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
}

// All compile and KSP tasks must wait for the common-metadata KSP pass
tasks.matching { task ->
    task.name != "kspCommonMainKotlinMetadata" &&
        (task.name.startsWith("compile") && task.name.contains("Kotlin") ||
            task.name.startsWith("ksp"))
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Disable build caching for the KSP task (source dir registration is unreliable in cache)
tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    outputs.cacheIf { false }
    val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
    outputs.upToDateWhen { outDir.get().asFile.exists() }
}
```

---

## Quick Start

Wrap your root composable with `SidekickShell` and pass your list of plugins:

```kotlin
@Composable
fun App() {
    val networkPlugin = remember { NetworkMonitorPlugin() }

    MaterialTheme {
        SidekickShell(plugins = listOf(networkPlugin)) {
            // your app content
        }
    }
}
```

A small bug-report FAB appears in the bottom-right corner. Tap it to open the Sidekick panel.

---

## Plugins

### Network Monitor

The Network Monitor captures and displays all HTTP traffic made through a supported client.

#### Setup

Add the network monitor and Ktor integration modules, then install the plugin on your `HttpClient`:

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor)
}
```

Create the plugin and pass it to `SidekickShell`:

```kotlin
val networkPlugin = remember { NetworkMonitorPlugin() }

SidekickShell(plugins = listOf(networkPlugin)) { ... }
```

#### Configuration

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor) {
        // Maximum characters captured per request/response body (default: 65 536)
        maxContentLength = 32_768

        // Redact sensitive headers
        sanitizeHeader { name -> name.equals("Authorization", ignoreCase = true) }
        sanitizeHeader(placeholder = "<token>") { name -> name.equals("X-Api-Key", ignoreCase = true) }

        // Exclude requests from being recorded
        filter { request -> request.url.host == "internal.metrics.local" }
    }
}
```

#### Retention

Control how long calls are kept in the database:

```kotlin
NetworkMonitorPlugin(retentionMs = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|---|---|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

#### UI

The Network Monitor panel adapts to the available screen width:

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a request to navigate to its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

Each request shows the HTTP method badge, host, path, status code (color-coded), and duration. The detail view has Request and Response tabs with copyable headers and pretty-printed JSON bodies.

---

### Preferences

The Preferences plugin exposes typed app settings in the Sidekick panel. Use the KSP annotation processor to generate the required boilerplate automatically.

#### Defining Preferences

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

#### Generated Code

KSP generates two classes from the annotated class:

**`AppPreferencesAccessor`** — reactive state for reading and writing preferences:

```kotlin
// Reading (from any coroutine scope or collectAsState in Compose)
val darkMode: StateFlow<Boolean>
val apiUrl: StateFlow<String>
val timeout: StateFlow<Int>

// Writing
suspend fun setDarkMode(value: Boolean)
suspend fun setApiUrl(value: String)
suspend fun setTimeout(value: Int)
```

**`AppPreferencesPlugin`** — the `SidekickPlugin` implementation, ready to pass to `SidekickShell`.

#### Usage

```kotlin
@Composable
fun App() {
    val prefsPlugin = remember { AppPreferencesPlugin() }
    val darkMode by prefsPlugin.accessor.darkMode.collectAsState()

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        SidekickShell(plugins = listOf(prefsPlugin)) {
            // your app content
        }
    }
}
```

#### UI

The Preferences panel adapts to screen width:

| Width | Layout |
|---|---|
| < 600 dp | Single-column list with inline editors |
| 600–840 dp | 2-column card grid |
| ≥ 840 dp | 3-column card grid |

Each card shows the preference type badge (BOOL / STR / INT / etc.), label, and an inline editor. Boolean cards have a toggle switch; string and number cards have an `OutlinedTextField` with a Save button that enables only when the value is dirty.

#### Manual Setup (without KSP)

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

---

## Custom Theming

### Automatic Theme Inheritance

By default, `SidekickShell` automatically picks up the host app's `MaterialTheme`:

- **Host has a custom `MaterialTheme`** → Sidekick uses those colors.
- **Host uses M3 defaults (or no `MaterialTheme`)** → Sidekick falls back to its own dark indigo scheme (`SidekickDefaultColorScheme`).

The host app's content is rendered **outside** Sidekick's theme and is never affected by it.

```kotlin
// Sidekick automatically uses your brand colors
MaterialTheme(colorScheme = myBrandColorScheme) {
    SidekickShell(plugins = plugins) {
        MyAppContent()
    }
}
```

### Overriding HTTP Badge and Status Colors

To customize the semantic colors used for HTTP method badges and status chips without changing the Material color scheme, pass a `SidekickColors` instance:

```kotlin
SidekickShell(
    plugins = plugins,
    sidekickColors = sidekickColors(
        httpGet    = Color(0xFF1976D2),
        httpPost   = Color(0xFF388E3C),
        httpPut    = Color(0xFFF57C00),
        httpDelete = Color(0xFFD32F2F),
        httpPatch  = Color(0xFF7B1FA2),
    ),
) {
    MyAppContent()
}
```

All parameters have sensible defaults derived from the resolved `MaterialTheme`. Only override what you need.

| Parameter | Used for |
|---|---|
| `httpGet` | GET method badge |
| `httpPost` | POST method badge |
| `httpPut` | PUT method badge |
| `httpDelete` | DELETE method badge |
| `httpPatch` | PATCH method badge |
| `httpOther` | Any other method |
| `onHttpBadge` | Text on method badges |
| `statusSuccess` | 2xx status chips |
| `statusRedirect` | 3xx status chips |
| `statusClientError` | 4xx status chips |
| `statusServerError` | 5xx status chips |
| `statusPending` | In-flight request indicator |
| `statusNetworkError` | Network error chip |
| `onStatusChip` | Text on status chips |

### Forcing a Specific Theme

Wrap with `SidekickTheme` to bypass auto-detection entirely and force a specific color scheme:

```kotlin
SidekickTheme(colorScheme = myForcedColorScheme) {
    SidekickShell(plugins = plugins) {
        MyAppContent()
    }
}
```

---

## Creating a Custom Plugin

Implement the `SidekickPlugin` interface:

```kotlin
class LogsPlugin : SidekickPlugin {
    override val id: String = "my-app.logs"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.Default.Article

    @Composable
    override fun Content() {
        // Your plugin UI — full Compose, full Material 3
        LazyColumn(Modifier.fillMaxSize()) {
            items(LogBuffer.entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.message) },
                    supportingContent = { Text(entry.tag, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}
```

Then pass it to `SidekickShell` alongside any other plugins:

```kotlin
SidekickShell(plugins = listOf(networkPlugin, prefsPlugin, LogsPlugin())) {
    MyAppContent()
}
```

### Guidelines

- **`id`** must be unique across all plugins. Use a reverse-domain prefix (e.g. `"com.myapp.logs"`).
- **`Content()`** is called inside `SidekickTheme`, so `MaterialTheme.colorScheme` and `LocalSidekickColors.current` are available.
- The `Content()` composable fills the full plugin panel area. Use `Modifier.fillMaxSize()` on the root.
- For reactive state, use `StateFlow` collected with `collectAsState()`.
- For adaptive layouts, use `BoxWithConstraints` with breakpoints at 600 dp (medium) and 840 dp (expanded).

### Accessing Sidekick Semantic Colors

Use `LocalSidekickColors.current` inside your plugin content to access HTTP and status colors consistent with the rest of the Sidekick UI:

```kotlin
@Composable
override fun Content() {
    val colors = LocalSidekickColors.current
    Text("OK", color = colors.statusSuccess)
}
```

---

## Release Builds

Replace `core:debug` with `core:noop` in release builds. The no-op implementation replaces `SidekickShell` with a composable that simply renders `content()` — no FAB, no panel, no overhead:

```kotlin
// build.gradle.kts (Android)
dependencies {
    debugImplementation(projects.core.debug)
    releaseImplementation(projects.core.noop)
}

// build.gradle.kts (non-Android targets — configure per source set)
jvmMain.dependencies {
    // swap this manually or via a build flag
    implementation(projects.core.debug)
}
```

No code changes required — `SidekickShell` has the same signature in both modules.
