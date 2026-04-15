# Sidekick

**A Kotlin Multiplatform debug overlay SDK** for Android, iOS, Desktop (JVM), and Web (JS/Wasm).

Sidekick adds a floating button to your app during development that opens a panel with pluggable debug tools — network inspector, log viewer, preferences editor, and more. In release builds, a no-op module strips the overlay entirely with zero overhead.

<div style="text-align: center; margin: 2rem 0;">
  <a href="demo/index.html" class="md-button md-button--primary" style="margin-right: 0.5rem;">
    Live Demo
  </a>
  <a href="installation/" class="md-button">
    Get Started
  </a>
</div>

---

## Features

- **Floating overlay** — a FAB and slide-up panel that sits on top of your app
- **Pluggable** — built-in plugins for network monitoring, logs, and preferences; easy to add your own
- **Adaptive UI** — responsive list-detail layouts at 600 dp and 840 dp breakpoints
- **Zero release cost** — `core:noop` replaces the overlay with a passthrough composable
- **Compose Multiplatform** — single UI codebase across all platforms

## Built-in Plugins

| Plugin | What it does |
|--------|-------------|
| [Network Monitor](plugins/network-monitor.md) | Captures and displays all HTTP traffic via Ktor |
| [Log Monitor](plugins/log-monitor.md) | Displays app logs with level filtering and search |
| [Preferences](plugins/preferences.md) | Exposes typed settings in the panel with KSP code generation |
| [Custom Screens](plugins/custom-screens.md) | Wraps any Composable as a first-class debug screen |

## Quick Example

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

A small FAB appears in the bottom-right corner. Tap it to open the Sidekick panel.

---

Ready to add Sidekick to your project? Start with [Installation](installation.md).
