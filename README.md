# Sidekick

A Kotlin Multiplatform debug overlay SDK for Android, iOS, Desktop (JVM), and Web (JS/Wasm). Sidekick adds a pluggable debug panel to your app — network inspector, log viewer, preferences editor, and custom screens. The client app controls the trigger (FAB, gesture, etc.); `Sidekick` is just a composable you show and hide.

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Plugins](#plugins)
  - [Network Monitor](#network-monitor)
  - [Log Monitor](#log-monitor)
  - [Preferences](#preferences)
  - [Custom Screens](#custom-screens)
- [Theming](#theming)
- [Creating a Custom Plugin](#creating-a-custom-plugin)
- [Claude Code Skills](#claude-code-skills)
- [Release Builds](#release-builds)

---

## Installation

Sidekick is a multi-module library. Add the modules you need as dependencies in your app's `build.gradle.kts`.

### Core

Every app needs the core runtime (debug builds) and the no-op stub (release builds):

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}
```

> **`core:noop`** replaces `Sidekick()` with an empty composable — zero overhead in release builds.

For Desktop (JVM), add both explicitly since `debugImplementation` is Android-only:

```kotlin
jvmMain.dependencies {
    implementation(projects.core.runtime)
}
```

### Android Context (automatic)

`core:plugin-api` ships a `SidekickInitializer` `ContentProvider` that auto-initializes the library context at app startup via manifest merger — **no `Application.onCreate()` call needed**.

### Plugins

```kotlin
commonMain.dependencies {
    // Network monitor
    implementation(projects.plugins.networkMonitor.plugin)
    implementation(projects.plugins.networkMonitor.ktor) // Ktor integration

    // Log monitor
    implementation(projects.plugins.logMonitor.plugin)
    implementation(projects.plugins.logMonitor.kermit)   // Kermit integration (optional)

    // Preferences
    implementation(projects.plugins.preferences.api)

    // Custom screens
    implementation(projects.plugins.customScreens.api)
}
```

### KSP (for the Preferences code generator)

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

tasks.matching { task ->
    task.name != "kspCommonMainKotlinMetadata" &&
        (task.name.startsWith("compile") && task.name.contains("Kotlin") ||
            task.name.startsWith("ksp"))
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    outputs.cacheIf { false }
    val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
    outputs.upToDateWhen { outDir.get().asFile.exists() }
}
```

---

## Quick Start

The client app owns the FAB and visibility state. `Sidekick` only renders the panel:

```kotlin
@Composable
fun App() {
    val networkPlugin = remember { NetworkMonitorPlugin() }
    val plugins = remember(networkPlugin) { listOf(networkPlugin) }

    var sidekickVisible by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            MyAppContent()

            SmallFloatingActionButton(
                onClick = { sidekickVisible = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Default.BugReport, contentDescription = "Open Sidekick")
            }

            AnimatedVisibility(
                visible = sidekickVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Sidekick(
                    plugins = plugins,
                    onClose = { sidekickVisible = false },
                )
            }
        }
    }
}
```

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

Create the plugin and pass it to `Sidekick`:

```kotlin
val networkPlugin = remember { NetworkMonitorPlugin() }

Sidekick(plugins = listOf(networkPlugin), onClose = { ... })
```

#### Configuration

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor) {
        maxContentLength = ContentLength.Default

        sanitizeHeader { name -> name.equals("Authorization", ignoreCase = true) }
        sanitizeHeader(placeholder = "<token>") { name -> name.equals("X-Api-Key", ignoreCase = true) }

        filter { request -> request.url.host == "internal.metrics.local" }
    }
}
```

| `ContentLength` constant | Value |
|---|---|
| `ContentLength.Default` | 65,536 characters *(default)* |
| `ContentLength.Full` | `Int.MAX_VALUE` — no truncation |

#### Retention

```kotlin
NetworkMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|---|---|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

#### UI

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a request to navigate to its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

---

### Log Monitor

The Log Monitor captures and displays application log messages. It works with any logging SDK through the `LogCollector` interface, with built-in support for [Kermit](https://github.com/nicklockwood/Kermit) by Touchlab.

#### Modules

| Module | Purpose |
|---|---|
| `plugins:log-monitor:api` | Core data model, `LogMonitorStore`, `LogCollector` interface |
| `plugins:log-monitor:plugin` | Compose UI + `LogMonitorPlugin` |
| `plugins:log-monitor:kermit` | Kermit `LogWriter` bridge |

#### Basic Setup

```kotlin
val logPlugin = remember { LogMonitorPlugin() }

Sidekick(plugins = listOf(logPlugin), onClose = { ... })
```

#### Integrating with Kermit

```kotlin
val logPlugin = remember {
    LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_HOUR).also { plugin ->
        Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
    }
}
```

#### Integrating with Other Logging SDKs

```kotlin
fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
```

`LogMonitorStore` itself implements `LogCollector`, so you can pass it directly to your SDK's log writer or call `LogMonitorStore.record()` for more control.

#### Retention

```kotlin
LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|---|---|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

Maximum stored entries: 1,000 (oldest are pruned automatically).

#### UI

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a log entry to see its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

**List view** features:
- Color-coded level badges (V=gray, D=green, I=blue, W=amber, E=red, A=red)
- **Level filter chips** — toggle each log level on/off
- **Search** — filter by tag or message text
- Error count indicator

**Detail view** shows:
- Full message (copyable)
- Stacktrace (copyable, if present)
- Timestamp
- Metadata table (if present)

---

### Preferences

The Preferences plugin exposes typed app settings in the Sidekick panel. Use the KSP annotation processor to generate the required boilerplate automatically.

#### Defining Preferences

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

KSP generates two classes:

**`AppPreferencesAccessor`** — reactive state:

```kotlin
val darkMode: StateFlow<Boolean>
val apiUrl: StateFlow<String>
val timeout: StateFlow<Int>

suspend fun setDarkMode(value: Boolean)
suspend fun setApiUrl(value: String)
suspend fun setTimeout(value: Int)
```

**`AppPreferencesPlugin`** — the `SidekickPlugin` implementation, ready to pass to `Sidekick`.

#### Usage

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

#### UI

| Width | Layout |
|---|---|
| < 600 dp | Single-column list with inline editors |
| 600–840 dp | 2-column card grid |
| ≥ 840 dp | 3-column card grid |

#### Manual Setup (without KSP)

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

### Custom Screens

`CustomScreenPlugin` wraps any Composable as a first-class debug screen in the Sidekick overlay.

```kotlin
val featureFlagsScreen = remember {
    CustomScreenPlugin(
        id    = "com.myapp.feature-flags",
        title = "Feature Flags",
        icon  = Icons.Default.Flag,
    ) {
        FeatureFlagsScreen() // DI, ViewModels, CompositionLocals all work here
    }
}

Sidekick(plugins = listOf(featureFlagsScreen), onClose = { ... })
```

---

## Theming

By default `Sidekick` applies its own Material 3 color scheme, following the system dark-mode setting:

```kotlin
Sidekick(plugins = plugins, onClose = { ... }) // uses Sidekick's own theme
```

Pass `useSidekickTheme = false` to inherit the host app's ambient `MaterialTheme` instead:

```kotlin
MaterialTheme(colorScheme = myBrandColorScheme) {
    Sidekick(
        plugins = plugins,
        onClose = { sidekickVisible = false },
        useSidekickTheme = false,
    )
}
```

| `useSidekickTheme` | Result |
|--------------------|--------|
| `true` *(default)* | Sidekick's own light/dark Material 3 scheme |
| `false` | Inherits the host app's ambient `MaterialTheme` |

HTTP badge and status chip colors are derived automatically from `MaterialTheme.colorScheme` — no extra configuration needed.

---

## Creating a Custom Plugin

Implement the `SidekickPlugin` interface:

```kotlin
class LogsPlugin : SidekickPlugin {
    override val id: String = "my-app.logs"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.Default.Article

    @Composable
    override fun Content(navigateBackToList: () -> Unit) {
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

Pass it to `Sidekick`:

```kotlin
Sidekick(plugins = listOf(networkPlugin, prefsPlugin, LogsPlugin()), onClose = { ... })
```

### Guidelines

- **`id`** must be unique. Use a reverse-domain prefix (e.g. `"com.myapp.logs"`).
- **`Content()`** runs inside the active `MaterialTheme` — `MaterialTheme.colorScheme` is available.
- Use `Modifier.fillMaxSize()` on the root of `Content()`.
- For reactive state, use `StateFlow` collected with `collectAsState()`.

### Plugins with Singletons and ViewModels (Koin)

For plugins that need managed singletons (e.g. a database, a coroutine scope) or lifecycle-aware ViewModels, use an **isolated Koin context** — the same pattern used by `NetworkMonitorPlugin`.

Each plugin owns its own `koinApplication {}` instance so its DI graph never conflicts with the host app's Koin setup.

#### 1 — Add dependencies

```kotlin
// plugins/<name>/api/build.gradle.kts
commonMain.dependencies {
    api(libs.koin.core)           // exposes KoinContext to sibling modules
}
androidMain.dependencies {
    implementation(libs.koin.android)
}

// plugins/<name>/plugin/build.gradle.kts
commonMain.dependencies {
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
}
androidMain.dependencies {
    implementation(libs.koin.android)
}
```

#### 2 — Create the isolated Koin context (in the `api` module)

```kotlin
// plugins/<name>/api/.../di/<Name>KoinContext.kt
object <Name>KoinContext {
    val koinApp: KoinApplication = koinApplication {
        modules(<name>CoreModule)
    }
    val koin get() = koinApp.koin

    private var viewModelModuleLoaded = false

    fun getDefaultStore(): <Name>Store = koin.get()

    fun loadViewModelModule(module: Module) {
        if (!viewModelModuleLoaded) {
            viewModelModuleLoaded = true
            koinApp.koin.loadModules(listOf(module))
        }
    }
}

internal val <name>CoreModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    singleOf(::<Name>Store)
}
```

#### 3 — Add a ViewModel and its Koin module (in the `plugin` module)

```kotlin
// plugins/<name>/plugin/.../di/<Name>Module.kt
internal val <name>ViewModelModule = module {
    viewModelOf(::<Name>ViewModel)
}

// plugins/<name>/plugin/.../<Name>ViewModel.kt
internal class <Name>ViewModel(private val store: <Name>Store) : ViewModel() {
    val items = store.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun clear() { viewModelScope.launch { store.clear() } }
}
```

#### 4 — Wire it in the plugin

```kotlin
class <Name>Plugin : SidekickPlugin {
    init {
        <Name>KoinContext.loadViewModelModule(<name>ViewModelModule)
    }

    @Composable
    override fun Content(navigateBackToList: () -> Unit) {
        KoinIsolatedContext(context = <Name>KoinContext.koinApp) {
            val viewModel: <Name>ViewModel = koinViewModel()
            val items by viewModel.items.collectAsStateWithLifecycle()
            <Name>Content(items = items, onClear = viewModel::clear, onBack = navigateBackToList)
        }
    }
}
```

The `/create-plugin` Claude Code skill scaffolds all of this automatically.

---

## Claude Code Skills

### `/setup-sidekick` — Setup wizard & preferences migration

An interactive wizard that handles the full Sidekick onboarding flow:

- Adds `core:runtime` / `core:noop` dependencies with the correct debug/release split.
- Prompts you to choose plugins and adds only the modules you need.
- Wires `Sidekick` into your root composable with FAB + `AnimatedVisibility`.
- **Migrates an existing DataStore preferences class** to the `@SidekickPreferences` annotation processor.

```
/setup-sidekick
```

### `/create-plugin` — Scaffold a new plugin module

Creates a new `plugins/<name>/api` module: `build.gradle.kts`, base `SidekickPlugin` class, and `settings.gradle.kts` registration.

```
/create-plugin my-feature-flags
```

---

## Release Builds

Replace `core:runtime` with `core:noop` in release builds. The no-op `Sidekick()` does nothing — no panel, no overhead:

```kotlin
// build.gradle.kts (Android)
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}

// Non-Android targets
jvmMain.dependencies {
    implementation(projects.core.runtime) // swap to core:noop for production
}
```

No code changes required — `Sidekick()` has the same signature in both modules.
