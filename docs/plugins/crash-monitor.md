# Crash Monitor

Captures fatal crashes and handled exceptions. Fatals are written to disk before the process dies, so you can read them on the next launch.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

## Features

- **Survives the restart** — fatals are persisted before the process dies and reloaded at startup.
- **Non-fatals too** — record exceptions you caught but still want to see.
- **Readable traces** — your own frames are highlighted, framework frames dimmed.
- **Unread badge** — count of crashes since you last opened the panel.
- **Share** — export one crash or the whole list.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:crash-monitor:api` | Models, store, per-platform crash hooks. |
| `:plugins:crash-monitor:ui` | `CrashMonitorPlugin` and its screens. |
| `:plugins:crash-monitor:noop` | Release stub. No handler installed, nothing written. |

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
    Plugins are constructed when the overlay is first composed, which is too late to catch a
    startup crash. Constructing `CrashMonitorPlugin` does not install the handler.

`appPackagePrefix` marks which frames are yours. Omit it and every frame is treated as library code.

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

This plugin uses no database. At crash time the process is shutting down, so coroutines and Room are unsafe to use. The fatal path is a synchronous append to a plain file, written before the in-memory list, since only the file outlives the process.

At most 50 records are kept and read whole at startup. Web uses `localStorage` and behaves the same.

## Platform behaviour

| Target | Hook | Note |
|---|---|---|
| Android / JVM | `Thread.setDefaultUncaughtExceptionHandler` | Chained to the previous handler, so Crashlytics, Sentry and the system crash dialog keep working. |
| iOS | `setUnhandledExceptionHook` | Catches Kotlin/Native failures. `NSSetUncaughtExceptionHandler` only sees Objective-C exceptions, which a Kotlin app rarely throws, so it is not used. |
| JS / WasmJS | `error` + `unhandledrejection` | Both are needed. A crashing coroutine surfaces as a rejected promise, which `onerror` does not see. |

## Release builds

Swap `crash-monitor-ui` for `crash-monitor-noop`. See [Release Builds](../release-builds.md). The noop installs no handler, leaving it to your production crash reporter.
