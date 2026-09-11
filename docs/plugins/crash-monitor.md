# Crash Monitor

Captures crashes so you can read them **after** the restart that followed them. A crash you can only see with a debugger attached is a crash you have already failed to reproduce.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

## Features

- **Survives the restart** — fatal crashes are written to disk before the process dies and reloaded on next launch.
- **Non-fatals too** — record exceptions you caught but still want to see.
- **Readable traces** — frames from your own package are highlighted, framework frames dimmed.
- **Unread badge** — the plugin card shows how many crashes landed since you last looked.
- **Share** — export one crash or the whole list through the platform share sheet.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:crash-monitor:api` | Models, store, per-platform crash hooks. |
| `:plugins:crash-monitor:ui` | `CrashMonitorPlugin` and its screens. |
| `:plugins:crash-monitor:noop` | Release stub — no handler installed, nothing written. |

## Setup

### 1. Add dependencies

```kotlin
commonMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:<bom-version>"))
    implementation("dev.parez.sidekick:crash-monitor-ui")
}
```

### 2. Install the handler early

```kotlin
// Android — Application.onCreate, after ApplicationContextHolder is ready
CrashMonitor.install(appPackagePrefix = "com.acme.app")

// Desktop / iOS — the first thing in main()
CrashMonitor.install(appPackagePrefix = "com.acme.app")
```

!!! warning "Install from your app's entry point, not the plugin"
    Plugins are constructed when the overlay is first composed. That is far too late to catch a
    startup crash — which is exactly the crash you most want captured. Constructing
    `CrashMonitorPlugin` deliberately does **not** install the handler.

`appPackagePrefix` decides which stack frames are marked as yours. Omit it and every frame is treated as library code, which makes the trace much harder to scan.

### 3. Register the plugin

```kotlin
val crashPlugin = remember { CrashMonitorPlugin() }

Sidekick(plugins = listOf(crashPlugin, /* … */))
```

### Recording handled exceptions

```kotlin
runCatching { risky() }
    .onFailure { CrashMonitor.recordNonFatal(it, origin = "sync-worker") }
```

## How it stores crashes

Unlike the network and log monitors, this plugin uses **no database**. At crash time the process is being torn down, so coroutines, Room and the invalidation tracker are all unsafe to reach for. The fatal path is a synchronous append to a plain file, written before the in-memory list is updated — the file is the only copy that outlives the process.

At most 50 records are kept. They are read whole at startup, which also means this plugin behaves identically on web (backed by `localStorage` there).

## Platform behaviour

| Target | Hook | Note |
|---|---|---|
| Android / JVM | `Thread.setDefaultUncaughtExceptionHandler` | **Chained** to whatever was installed before, so Crashlytics / Sentry / the system crash dialog keep working. |
| iOS | `setUnhandledExceptionHook` | Catches Kotlin/Native failures. `NSSetUncaughtExceptionHandler` is *not* used — it only sees Objective-C exceptions, which a Kotlin app rarely throws. |
| JS / WasmJS | `error` + `unhandledrejection` | Both are needed: a crashing coroutine surfaces as a rejected promise, which `onerror` never sees. |

## Release builds

Swap `crash-monitor-ui` for `crash-monitor-noop` — see [Release Builds](../release-builds.md). In the noop variant no handler is installed at all, so your production crash reporter keeps the handler entirely to itself.
