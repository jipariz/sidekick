# Log Monitor

The Log Monitor captures and displays application log messages. It works with any logging SDK through the `LogCollector` interface, with built-in support for [Kermit](https://github.com/touchlab/Kermit) by Touchlab.

## Modules

| Module | Purpose |
|--------|---------|
| `plugins:log-monitor:api` | Core data model, `LogMonitorStore`, `LogCollector` interface |
| `plugins:log-monitor:plugin` | Compose UI + `LogMonitorPlugin` |
| `plugins:log-monitor:kermit` | Kermit `LogWriter` bridge |

## Basic Setup

```kotlin
val logPlugin = remember { LogMonitorPlugin() }

SidekickShell(plugins = listOf(logPlugin)) { ... }
```

## Kermit Integration

The Kermit bridge forwards all Kermit log calls to the Log Monitor. Configure it once at app startup:

```kotlin
val logPlugin = remember {
    LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_HOUR).also { plugin ->
        Logger.setLogWriters(platformLogWriter(), LogMonitorLogWriter(plugin.store))
    }
}
```

All `Logger.d(...)`, `Logger.i(...)`, `Logger.e(...)` calls now appear in the Sidekick log panel automatically.

## Custom Logging SDK Integration

Implement the `LogCollector` interface to bridge any logging library:

```kotlin
fun interface LogCollector {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
```

`LogMonitorStore` itself implements `LogCollector`, so you can pass it directly to your SDK's log writer. Alternatively, call `LogMonitorStore.record()` for more control:

```kotlin
LogMonitorStore.record(
    level = LogLevel.INFO,
    tag = "MyTag",
    message = "Something happened",
    throwable = null,
    metadata = mapOf("key" to "value"), // optional
)
```

## Retention

```kotlin
LogMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|----------|----------|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

Maximum stored entries: **1,000** (oldest are pruned automatically).

## UI

The Log Monitor panel adapts to the available screen width:

| Width | Layout |
|-------|--------|
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
