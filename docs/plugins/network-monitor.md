# Network Monitor

The Network Monitor captures and displays all HTTP traffic made through a supported client.

## Setup

Add the modules:

```kotlin
commonMain.dependencies {
    implementation(projects.plugins.networkMonitor.plugin)
    implementation(projects.plugins.networkMonitor.ktor)
}
```

Install the Ktor plugin on your `HttpClient`:

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor)
}
```

Create the plugin and pass it to `Sidekick`:

```kotlin
val networkPlugin = remember { NetworkMonitorPlugin() }

Sidekick(
    plugins = listOf(networkPlugin),
    onClose = { sidekickVisible = false },
)
```

## Configuration

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor) {
        // Maximum characters captured per request/response body
        maxContentLength = ContentLength.Default

        // Redact sensitive headers
        sanitizeHeader { name -> name.equals("Authorization", ignoreCase = true) }
        sanitizeHeader(placeholder = "<token>") { name ->
            name.equals("X-Api-Key", ignoreCase = true)
        }

        // Exclude requests from being recorded
        filter { request -> request.url.host == "internal.metrics.local" }
    }
}
```

### `ContentLength` constants

| Constant | Value |
|----------|-------|
| `ContentLength.Default` | 65,536 characters *(default)* |
| `ContentLength.Full` | `Int.MAX_VALUE` — no truncation |

## Retention

Control how long calls are kept in the database:

```kotlin
NetworkMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|----------|----------|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

## UI

The Network Monitor panel adapts to the available screen width:

| Width | Layout |
|-------|--------|
| < 600 dp | Single pane — tap a request to navigate to its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

Each request shows the HTTP method badge, host, path, status code (color-coded), and duration. The detail view has **Request** and **Response** tabs with copyable headers and pretty-printed JSON bodies.

Badge and chip colors are derived from the active `MaterialTheme.colorScheme`. See [Theming](../theming.md#http-badge-and-status-colors) for the full mapping.
