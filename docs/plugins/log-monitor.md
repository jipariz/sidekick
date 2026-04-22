# Log Monitor

The Log Monitor captures and displays application log messages. It works with any logging library through the `LogCollector` interface, with built-in support for [Kermit](https://github.com/touchlab/Kermit) by Touchlab.

## Modules

| Module | Purpose |
|--------|---------|
| `plugins:log-monitor:api` | Core data model, `LogMonitorStore`, `LogCollector` interface |
| `plugins:log-monitor:plugin` | Compose UI + `LogMonitorPlugin` |
| `plugins:log-monitor:kermit` | Kermit `LogWriter` bridge |

---

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
            implementation(projects.plugins.logMonitor.plugin)
            implementation(projects.plugins.logMonitor.kermit) // optional — Kermit bridge
        }
    }
}
```

Omit `logMonitor.kermit` if you are not using Kermit and plan to use a [custom logging SDK](#custom-logging-sdk-integration).

### 2. Add the plugin to Sidekick

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

        AnimatedVisibility(visible = sidekickVisible, ...) {
            Sidekick(
                plugins = listOf(logPlugin),
                onClose = { sidekickVisible = false },
            )
        }
    }
}
```

---

## Kermit Integration

The Kermit bridge forwards all Kermit log calls to the Log Monitor. Configure it once at app startup — typically in your `App` composable or `Application.onCreate()`:

```kotlin
val logPlugin = remember {
    LogMonitorPlugin().also { plugin ->
        Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
    }
}
```

All `Logger.d(...)`, `Logger.i(...)`, `Logger.e(...)` calls now appear in the Sidekick log panel automatically.

---

## Custom Logging SDK Integration

Implement the `LogCollector` interface to bridge any logging library:

```kotlin
fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
```

`LogMonitorStore` itself implements `LogCollector`, so you can pass `logPlugin.store` directly to your SDK's log writer:

```kotlin
val logPlugin = remember { LogMonitorPlugin() }

// Pass the store as a LogCollector to your SDK's writer
MyLoggingSDK.addWriter(logPlugin.store)
```

For more control, call `LogMonitorStore.record()` directly:

```kotlin
logPlugin.store.record(
    level = LogLevel.INFO,
    tag = "MyTag",
    message = "Something happened",
    throwable = null,
    metadata = mapOf("requestId" to "abc-123"), // optional
)
```

### Timber example (Android)

```kotlin
class SidekickTree(private val store: LogMonitorStore) : Timber.Tree() {
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
        store.record(level = level, tag = tag ?: "App", message = message, throwable = t)
    }
}
```

Plant the tree at startup:

```kotlin
val logPlugin = remember { LogMonitorPlugin() }

LaunchedEffect(Unit) {
    Timber.plant(SidekickTree(logPlugin.store))
}
```

!!! note
    `Timber` is Android-only. For multiplatform projects, prefer [Kermit](#kermit-integration).

---

## Retention

```kotlin
LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|---|---|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

Maximum stored entries: **1,000** (oldest are pruned automatically).

---

## UI

The Log Monitor panel adapts to the available screen width:

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a log entry to see its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

**List view** features:

- Color-coded level badges (V=gray, D=green, I=blue, W=amber, E=red, A=red)
- Level filter chips — toggle each log level on/off
- Search — filter by tag or message text
- Error count indicator

**Detail view** shows:

- Full message (copyable)
- Stacktrace (copyable, if present)
- Timestamp
- Metadata table (if present)
