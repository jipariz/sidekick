# Creating a Custom Plugin

Build your own first-class Sidekick plugin by implementing the `SidekickPlugin` interface from `:core:plugin-api`. Use this when you want a self-contained module with its own DI scope, data layer, and UI — for example, a Room-backed event log, a feature-flag editor with a remote backend, or a runtime DSL inspector. For simpler one-off screens, prefer [Custom Screens](custom-screen.md).

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

<sub>Your plugin inherits the targets you publish for. The `SidekickPlugin` interface itself is `commonMain`-only.</sub>

## Features

- **One interface** — `id`, `title`, `icon`, `@Composable Content()`.
- **No required dependencies** — just `:core:plugin-api`. Bring your own data layer, DI, and state management.
- **Lifecycle-aware** — implement `SidekickLifecycleAware` if you need `onAttach` / `onDetach` hooks.
- **Badged** — implement `SidekickBadged` to surface an unread count on the plugin's card, and on your own entry point.
- **Back-navigation built in** — call `LocalSidekickBackNavigator.current` to return to the plugin grid.

## Modules

| Module | Purpose |
|---|---|
| `:core:plugin-api` | `SidekickPlugin`, `SidekickAppInfo`, `LocalSidekickBackNavigator`, `SidekickLifecycleAware`, `SidekickBadged`, `SidekickShare`. |
| Your own module | Implement `SidekickPlugin`, ship the data layer + UI. |

## Setup

### 1. Add dependency

Your plugin module should compile against `plugin-api`:

```kotlin
// your-plugin/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:2026.08.28"))
            implementation("dev.parez.sidekick:plugin-api")  // version from BOM
        }
    }
}
```

### 2. Implement `SidekickPlugin`

```kotlin
class LogsPlugin : SidekickPlugin {
    override val id: String = "com.myapp.logs"
    override val title: String = "Logs"
    override val icon: ImageVector = Icons.Default.Article

    @Composable
    override fun Content() {
        LazyColumn(Modifier.fillMaxSize()) {
            items(LogBuffer.entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.message) },
                    supportingContent = {
                        Text(entry.tag, style = MaterialTheme.typography.labelSmall)
                    },
                )
            }
        }
    }
}
```

### 3. Wire into Sidekick

Pass it alongside any other plugins:

```kotlin
Sidekick(
    plugins = listOf(networkPlugin, prefsPlugin, LogsPlugin()),
    actions = {
        IconButton(onClick = { sidekickVisible = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    },
)
```

## Configuration

| Member | Constraint |
|---|---|
| `id: String` | Must be unique across all plugins passed to `Sidekick`. Prefer reverse-domain (e.g. `"com.myapp.logs"`). |
| `title: String` | Shown in the plugin-grid card and the screen header. |
| `icon: ImageVector` | Shown in the plugin-grid card. Material icons recommended for consistency. |
| `@Composable Content()` | Renders inside the active `MaterialTheme`. Fills the plugin panel area — use `Modifier.fillMaxSize()` on the root. |

## UI

- `Content()` runs under the host's active `MaterialTheme` (or Sidekick's, depending on `useSidekickTheme`). `MaterialTheme.colorScheme` is available.
- For reactive state, use `StateFlow` collected with `collectAsState()` or `collectAsStateWithLifecycle()`.
- For adaptive layouts, use `BoxWithConstraints` or the M3 Adaptive `ListDetailPaneScaffold` with breakpoints at 600 dp (medium) and 840 dp (expanded). Both built-in monitor plugins use this pattern.
- To navigate back to the plugin grid programmatically (e.g. a "Close" button inside your screen), call `LocalSidekickBackNavigator.current()`.

## Advanced

### Lifecycle hooks

Implement `SidekickLifecycleAware` if your plugin needs to know when the panel opens or closes (e.g. start / stop polling):

```kotlin
class LogsPlugin : SidekickPlugin, SidekickLifecycleAware {
    override fun onAttach() { startTailing() }
    override fun onDetach() { stopTailing() }
    /* … */
}
```

### Unread badge

Implement `SidekickBadged` to report how many things arrived since the user last looked. Sidekick
draws it on your plugin's card, and — more usefully — the host app can read the same value to badge
its own entry point, which is otherwise impossible since the host owns the FAB:

```kotlin
class MyPlugin : SidekickPlugin, SidekickBadged, SidekickLifecycleAware {
    private val _badge = mutableStateOf<Int?>(null)
    override val badge: State<Int?> = _badge

    override fun onAttach() { _badge.value = null }   // opened: mark read
    /* … */
}

// In the host app:
val count by myPlugin.badge
BadgedBox(badge = { if (count != null) Badge { Text("$count") } }) { Fab() }
```

Back the value with a **monotonic counter**, not a retained row count. Stores that cap their history
stop changing size once full, so a count-based badge silently stops reporting new activity at exactly
the point the plugin is busiest.

### Sharing data out

`SidekickShare.share(text, subject)` hands a block of text to the platform: an intent chooser on
Android, a share sheet on iOS, a save-or-clipboard prompt on desktop, a download in the browser.
Clipboard moves one value; this moves a log dump.

```kotlin
IconButton(onClick = { SidekickShare.share(report(), subject = "my-plugin") }) {
    Icon(Icons.Default.Share, contentDescription = "Export")
}
```

Call it from the main thread — it is best-effort and swallows failures rather than throwing into a
debug overlay.

### Scaffolding with Claude Code

Use the [`/create-plugin`](../claude-code-skills.md#create-plugin) skill to scaffold a new plugin module from scratch — it creates the `build.gradle.kts`, base implementation class, and registers the module in `settings.gradle.kts`.

## See also

- [Custom Screens](custom-screen.md) — simpler wrapper when you just want a Composable in the panel.
- [Network Monitor](network-monitor.md) and [Log Monitor](log-monitor.md) for reference implementations.
