# Network Monitor

The Network Monitor captures and displays all HTTP traffic made through your app. It has built-in support for [Ktor](https://ktor.io/), and exposes a low-level `NetworkMonitorStore` API to integrate any HTTP client.

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
            implementation(projects.plugins.networkMonitor.plugin)
            implementation(projects.plugins.networkMonitor.ktor) // Ktor integration
        }
    }
}
```

If you are using a custom HTTP client instead of Ktor, omit `networkMonitor.ktor` and follow [Custom HTTP Client Integration](#custom-http-client-integration).

### 2. Add the plugin to Sidekick

The client app owns the FAB and visibility state. `Sidekick` only renders the panel. See [Quick Start](../quick-start.md) for the full pattern:

```kotlin
@Composable
fun App() {
    val networkPlugin = remember { NetworkMonitorPlugin() }
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
                plugins = listOf(networkPlugin),
                onClose = { sidekickVisible = false },
            )
        }
    }
}
```

### 3. Install on your HTTP client

Pick your integration: [Ktor](#ktor-integration) or [custom HTTP client](#custom-http-client-integration).

---

## Ktor Integration

Install `NetworkMonitorKtor` on your `HttpClient`:

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor)
}
```

All requests made through this client are automatically captured.

### Configuration

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

        // Exclude specific requests from being recorded
        filter { request -> request.url.host == "internal.metrics.local" }
    }
}
```

| `ContentLength` constant | Value |
|---|---|
| `ContentLength.Default` | 65,536 characters *(default)* |
| `ContentLength.Full` | `Int.MAX_VALUE` — no truncation |

---

## Custom HTTP Client Integration

If you are not using Ktor, record calls manually via `NetworkMonitorStore`. The store is completely client-agnostic — it is the same store the Ktor plugin writes to.

### 1. Get the store

```kotlin
val store = NetworkMonitorKoinContext.getDefaultStore()
```

### 2. Record each call

Wrap your HTTP call with the store's recording methods:

```kotlin
val callId = uuid4().toString() // unique per request

// Before the request
store.recordRequest(
    id = callId,
    url = request.url.toString(),
    method = request.method,
    headers = request.headers.toMap(),
    body = request.body?.readText(),
    timestamp = currentTimeMillis(),
)

try {
    val response = yourHttpClient.execute(request)

    // Record status code + headers
    store.recordResponse(
        id = callId,
        code = response.statusCode,
        headers = response.headers.toMap(),
        timestamp = currentTimeMillis(),
    )

    // Record the body (skip for binary responses)
    store.recordResponseBody(id = callId, body = response.bodyAsText())

} catch (e: Exception) {
    store.recordError(id = callId, error = e)
    throw e
}
```

### OkHttp example

Wrap the store calls in an `Interceptor` to keep your call sites clean:

```kotlin
class NetworkMonitorInterceptor(
    private val store: NetworkMonitorStore,
    private val scope: CoroutineScope,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val callId = UUID.randomUUID().toString()
        val requestTime = System.currentTimeMillis()

        scope.launch {
            store.recordRequest(
                id = callId,
                url = request.url.toString(),
                method = request.method,
                headers = request.headers.toMap(),
                body = request.body?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    buffer.readUtf8()
                },
                timestamp = requestTime,
            )
        }

        return try {
            val response = chain.proceed(request)
            val responseTime = System.currentTimeMillis()
            val bodyText = response.peekBody(Long.MAX_VALUE).string()

            scope.launch {
                store.recordResponse(
                    id = callId,
                    code = response.code,
                    headers = response.headers.toMap(),
                    timestamp = responseTime,
                )
                store.recordResponseBody(id = callId, body = bodyText)
            }
            response
        } catch (e: IOException) {
            scope.launch { store.recordError(id = callId, error = e) }
            throw e
        }
    }
}
```

Register the interceptor on your client:

```kotlin
val store = NetworkMonitorKoinContext.getDefaultStore()

val client = OkHttpClient.Builder()
    .addInterceptor(NetworkMonitorInterceptor(store, coroutineScope))
    .build()
```

!!! note
    `OkHttp` is Android/JVM-only. For truly multiplatform code, consider the [Ktor integration](#ktor-integration).

---

## Retention

Control how long calls are kept:

```kotlin
NetworkMonitorPlugin(retentionPeriod = RetentionPeriod.ONE_DAY)
```

| Constant | Duration |
|---|---|
| `RetentionPeriod.ONE_HOUR` | 1 hour *(default)* |
| `RetentionPeriod.ONE_DAY` | 24 hours |
| `RetentionPeriod.ONE_WEEK` | 7 days |
| `RetentionPeriod.FOREVER` | Never purged |

---

## UI

The Network Monitor panel adapts to the available screen width:

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a request to navigate to its detail |
| 600–840 dp | Two panes at 40/60 split |
| ≥ 840 dp | Two panes — list fixed at 360 dp |

Each request shows the HTTP method badge, host, path, status code (color-coded), and duration. The detail view has **Request** and **Response** tabs with copyable headers and pretty-printed JSON bodies.

Badge and chip colors are derived from the active `MaterialTheme.colorScheme`. See [Theming](../theming.md#http-badge-and-status-colors) for the full mapping.
