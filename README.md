# Sidekick

A Kotlin Multiplatform debug overlay SDK for Android, iOS, Desktop (JVM), and Web (JS/Wasm). Sidekick adds a floating debug panel to your app during development — network inspector, log viewer, preferences editor, and custom screens. In release builds, a no-op module strips the overlay entirely with zero overhead.

---

## Quick Install

### 1. Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)  // no-op in release — zero cost
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.plugins.networkMonitor.plugin)
            implementation(projects.plugins.networkMonitor.ktor)
            implementation(projects.plugins.logMonitor.plugin)
            implementation(projects.plugins.preferences.api)
        }
    }
}
```

Add only the plugins you need. See [Installation](docs/installation.md) for per-platform notes and KSP setup.

### 2. Wire into your composable

The client app owns the FAB and visibility state. `Sidekick` only renders the panel:

```kotlin
@Composable
fun App() {
    val networkPlugin = remember { NetworkMonitorPlugin() }
    val logPlugin = remember { LogMonitorPlugin() }
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
                    plugins = listOf(networkPlugin, logPlugin),
                    onClose = { sidekickVisible = false },
                )
            }
        }
    }
}
```

---

## Built-in Plugins

| Plugin | What it does |
|--------|-------------|
| [Network Monitor](docs/plugins/network-monitor.md) | Captures and displays all HTTP traffic (Ktor built-in, any client via custom integration) |
| [Log Monitor](docs/plugins/log-monitor.md) | Displays app logs with level filtering and search (Kermit built-in, any SDK via `LogCollector`) |
| [Preferences](docs/plugins/preferences.md) | Exposes typed settings with KSP code generation or manual DataStore bridging |
| [Custom Screens](docs/plugins/custom-screens.md) | Wraps any Composable as a first-class debug screen |

---

→ **[Full documentation](docs/index.md)**

---

## Credits

The demo app uses data from [PokéAPI](https://pokeapi.co) — a free, open RESTful Pokémon API. See [pokeapi.co/about](https://pokeapi.co/about) for details.
