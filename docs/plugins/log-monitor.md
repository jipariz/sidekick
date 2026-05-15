# Log Monitor

View your app's logs without ADB or platform-specific consoles. Level filters, full-text search, copyable stacktraces, and metadata-aware entries. Built-in bridge for [Kermit](https://github.com/touchlab/Kermit) by Touchlab; any logging library plugs in via the `LogCollector` interface.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

## Features

- **Color-coded levels** — V / D / I / W / E / A badges adapt to the active theme.
- **Level filter chips** — toggle each level on / off independently.
- **Full-text search** — filter by tag or message text.
- **Error counter** — at-a-glance count of `ERROR`/`ASSERT` entries.
- **Copyable detail view** — message, stacktrace, timestamp, optional metadata.
- **Multi-library bridges** — Kermit out of the box; Timber, SLF4J, java.util.logging, anything that exposes a writer via the simple `LogCollector` interface.
- **Configurable retention** — auto-prune by `Duration`. Total entries capped at 1 000.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:log-monitor:api` | Core data model, `LogMonitorStore`, `LogCollector` interface. |
| `:plugins:log-monitor:plugin` | Compose UI + `LogMonitorPlugin` (the `SidekickPlugin` impl). |
| `:plugins:log-monitor:kermit` | Kermit `LogWriter` bridge. |

## Setup

### 1. Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation("dev.parez.sidekick:runtime:0.1.0")
    releaseImplementation("dev.parez.sidekick:noop:0.1.0")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:0.1.0"))
            implementation("dev.parez.sidekick:log-monitor-plugin")
            implementation("dev.parez.sidekick:log-monitor-kermit") // optional — Kermit bridge
        }
    }
}
```

Omit `log-monitor-kermit` if you're not using Kermit; see [Advanced › Custom logging library](#custom-logging-library).

For multi-module apps where the calling code is in a KMP library, see [Installation › Multi-module KMP app](../installation.md#multi-module-kmp-app) for the `compileOnly` / per-target split.

### 2. Wire into Sidekick

```kotlin
@Composable
fun App() {
    val logPlugin = remember { LogMonitorPlugin() }
    var sidekickVisible by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        MyAppContent()

        SmallFloatingActionButton(
            onClick = { sidekickVisible = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "Open Sidekick")
        }

        AnimatedVisibility(visible = sidekickVisible, /* enter, exit */) {
            Sidekick(
                plugins = listOf(logPlugin),
                actions = {
                    IconButton(onClick = { sidekickVisible = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        }
    }
}
```

### 3. Install the Kermit bridge

The bridge forwards every Kermit log call into the Log Monitor. Wire it once at app startup — typically in your `App` composable or `Application.onCreate()`:

```kotlin
val logPlugin = remember {
    LogMonitorPlugin().also {
        Logger.setLogWriters(
            platformLogWriter(),
            LogMonitorLogWriter(),       // defaults to the LogMonitorStore singleton
        )
    }
}
```

All `Logger.d(...)`, `Logger.i(...)`, `Logger.e(...)` calls now appear in the Sidekick log panel automatically.

#### Alternative: configure a custom `Logger` instance

If you already build a Kermit `Logger` in DI with `mutableLoggerConfigInit(...)` — typical when you have multiple writers (Crashlytics, an in-app DB, etc.) — add `LogMonitorLogWriter()` to the same writer list:

```kotlin
val config = mutableLoggerConfigInit(
    platformLogWriter(),
    DatabaseLogWriter(...),
    CrashlyticsLogWriter(),
    LogMonitorLogWriter(),
)
val logger = Logger(config = config, tag = "MyApp")
```

#### Conditional bridge in release builds

`:log-monitor-kermit` and `:log-monitor-plugin` are declared as `implementation`, so they ship in release builds alongside `:noop`. `Sidekick()` won't render the panel in release, but `LogMonitorLogWriter()` still records every Kermit call into the in-memory store. Gate the writer on build type:

```kotlin
val writers = buildList {
    add(platformLogWriter())
    add(CrashlyticsLogWriter())
    if (!appProperties.isRelease) add(LogMonitorLogWriter())
}
Logger(config = mutableLoggerConfigInit(*writers.toTypedArray()), tag = "MyApp")
```

## Configuration

```kotlin
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

LogMonitorPlugin(retentionPeriod = 24.hours)
```

| Example | Behaviour |
|---|---|
| `1.hours` *(default)* | Keep the last hour of entries. |
| `24.hours` | Keep the last day. |
| `7.days` | Keep the last week. |
| `Duration.INFINITE` | Never prune by age. |

The store also caps total entries at **1 000** (oldest pruned first), regardless of retention.

## UI

The panel adapts to the available width:

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap an entry to see its detail. |
| 600 – 840 dp | Two panes at 40 / 60 split. |
| ≥ 840 dp | Two panes — list fixed at 360 dp. |

**List view** features:

- Color-coded level badges (V = gray, D = green, I = blue, W = amber, E = red, A = red).
- Level filter chips — toggle each log level on / off.
- Search — filter by tag or message text.
- Error count indicator.

**Detail view** shows:

- Full message (copyable).
- Stacktrace (copyable, if present).
- Timestamp.
- Metadata table (if present).

## Advanced

### Custom logging library

Implement the `LogCollector` interface — or just pass the singleton `LogMonitorStore` directly to your writer, since the store implements `LogCollector`:

```kotlin
fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
```

```kotlin
val logPlugin = remember { LogMonitorPlugin() }
MyLoggingSDK.addWriter(LogMonitorStore) // LogMonitorStore implements LogCollector
```

For maximum control, call `LogMonitorStore.record(...)` directly:

```kotlin
LogMonitorStore.record(
    level = LogLevel.INFO,
    tag = "MyTag",
    message = "Something happened",
    throwable = null,
    metadata = mapOf("requestId" to "abc-123"), // optional
)
```

### Timber example (Android)

```kotlin
class SidekickTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.VERBOSE -> LogLevel.VERBOSE
            Log.DEBUG   -> LogLevel.DEBUG
            Log.INFO    -> LogLevel.INFO
            Log.WARN    -> LogLevel.WARN
            Log.ERROR   -> LogLevel.ERROR
            Log.ASSERT  -> LogLevel.ASSERT
            else        -> LogLevel.DEBUG
        }
        LogMonitorStore.record(level = level, tag = tag ?: "App", message = message, throwable = t)
    }
}
```

Plant the tree at startup:

```kotlin
val logPlugin = remember { LogMonitorPlugin() }

LaunchedEffect(Unit) {
    Timber.plant(SidekickTree())
}
```

!!! note
    Timber is Android-only. For multiplatform projects, prefer the [Kermit bridge](#3-install-the-kermit-bridge).

## See also

- [Network Monitor](network-monitor.md)
- [Custom plugin](custom-plugin.md)
