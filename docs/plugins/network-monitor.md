# Network Monitor

Capture every HTTP request and response your app makes, with searchable list, method filters, and a detail view that pretty-prints headers and JSON bodies. Built-in [Ktor](https://ktor.io/) integration; any other client plugs in via the low-level `NetworkMonitorStore` API.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

## Features

- **Searchable call list** — filter by URL, host, path, or response body.
- **Method chips** — toggle GET / POST / PUT / DELETE / PATCH on or off.
- **Color-coded status** — `2xx`, `3xx`, `4xx`, `5xx`, and pending requests render distinctly.
- **Request / response tabs** — copyable headers, pretty-printed JSON bodies.
- **Header sanitization** — drop or redact `Authorization`, `X-Api-Key`, anything you choose.
- **Request filtering** — skip specific hosts or routes from being recorded.
- **Body truncation** — cap captured body length to avoid log bloat.
- **Configurable retention** — auto-prune calls older than your chosen `Duration`.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:network-monitor:api` | SQLDelight data layer + `NetworkMonitorStore` (Paging-backed). |
| `:plugins:network-monitor:plugin` | Compose UI + `NetworkMonitorPlugin` (the `SidekickPlugin` impl). |
| `:plugins:network-monitor:ktor` | `NetworkMonitorKtor` Ktor `HttpClientPlugin` (optional). |

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

If you use a non-Ktor HTTP client, omit `networkMonitor.ktor` and see [Advanced › Custom HTTP client](#custom-http-client).

### 2. Wire into Sidekick

The client app owns the FAB and visibility state; `Sidekick` only renders the panel. See [Quick Start](../quick-start.md) for the full pattern:

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

        AnimatedVisibility(visible = sidekickVisible, /* enter, exit */) {
            Sidekick(
                plugins = listOf(networkPlugin),
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

### 3. Install on your Ktor client

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor)
}
```

Every request made through this client is captured automatically.

## Configuration

### Ktor plugin DSL

```kotlin
val httpClient = HttpClient {
    install(NetworkMonitorKtor) {
        // Truncate captured request/response bodies past this many characters.
        maxContentLength = ContentLength.Default

        // Redact sensitive headers (called once per header).
        sanitizeHeader { name -> name.equals("Authorization", ignoreCase = true) }
        sanitizeHeader(placeholder = "<token>") { name ->
            name.equals("X-Api-Key", ignoreCase = true)
        }

        // Exclude specific requests from being recorded.
        filter { request: HttpRequestBuilder -> request.url.host == "internal.metrics.local" }
    }
}
```

| Setting | Type | Default | Notes |
|---|---|---|---|
| `maxContentLength` | `Int` | `ContentLength.Default` (65 536) | Use `ContentLength.Full` (`Int.MAX_VALUE`) to disable truncation. |
| `sanitizeHeader(placeholder, predicate)` | DSL | — | Replaces matching header values with `placeholder` (default `"***"`). Call multiple times. |
| `filter(predicate)` | DSL | — | Predicate receives an `HttpRequestBuilder`. Requests where any registered predicate returns `true` are skipped. |

### Plugin retention

`NetworkMonitorPlugin` accepts a `kotlin.time.Duration`. Older calls are pruned on next `init`.

```kotlin
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

NetworkMonitorPlugin(retentionPeriod = 24.hours)
```

| Example | Behaviour |
|---|---|
| `1.hours` *(default)* | Keep the last hour of calls. |
| `24.hours` | Keep the last day. |
| `7.days` | Keep the last week. |
| `Duration.INFINITE` | Never prune. |

The store also caps total rows at **500** (oldest pruned first), regardless of retention.

## UI

The panel adapts to the available width:

| Width | Layout |
|---|---|
| < 600 dp | Single pane — tap a request to navigate to its detail. |
| 600 – 840 dp | Two panes at 40 / 60 split. |
| ≥ 840 dp | Two panes — list fixed at 360 dp. |

Each row shows the HTTP method badge, host, path, status code (color-coded), and duration. The detail view has **Request** and **Response** tabs with copyable headers and pretty-printed JSON. Badge and chip colors derive from the active `MaterialTheme.colorScheme` — see [Theming › HTTP badge and status colors](../theming.md#http-badge-and-status-colors).

## Advanced

### Custom HTTP client

If you're not on Ktor, record calls manually via `NetworkMonitorStore`. The store is client-agnostic and shared with the Ktor plugin.

**Get the store**

```kotlin
val store = NetworkMonitorKoinContext.getDefaultStore()
```

**Record each call**

```kotlin
val callId = uuid4().toString() // unique per request

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

    store.recordResponse(
        id = callId,
        code = response.statusCode,
        headers = response.headers.toMap(),
        timestamp = currentTimeMillis(),
    )
    store.recordResponseBody(id = callId, body = response.bodyAsText())

} catch (e: Throwable) {
    store.recordError(id = callId, error = e)
    throw e
}
```

### OkHttp interceptor (Android / JVM)

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

```kotlin
val store = NetworkMonitorKoinContext.getDefaultStore()

val client = OkHttpClient.Builder()
    .addInterceptor(NetworkMonitorInterceptor(store, coroutineScope))
    .build()
```

!!! note
    OkHttp is Android/JVM-only. For Compose Multiplatform code, prefer the Ktor integration above.

## See also

- [Theming › HTTP badge and status colors](../theming.md#http-badge-and-status-colors)
- [Log Monitor](log-monitor.md)
- [Custom plugin](custom-plugin.md)
