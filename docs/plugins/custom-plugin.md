# Creating a Custom Plugin

Implement the `SidekickPlugin` interface from `:core:plugin-api`:

```kotlin
class LogsPlugin : SidekickPlugin {
    override val id: String = "my-app.logs"
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

Pass it to `Sidekick` alongside any other plugins:

```kotlin
Sidekick(
    plugins = listOf(networkPlugin, prefsPlugin, LogsPlugin()),
    onClose = { sidekickVisible = false },
)
```

## Guidelines

- **`id`** must be unique across all plugins. Use a reverse-domain prefix (e.g. `"com.myapp.logs"`).
- **`Content()`** is called inside the active `MaterialTheme`, so `MaterialTheme.colorScheme` is available.
- The `Content()` composable fills the full plugin panel area. Use `Modifier.fillMaxSize()` on the root.
- For reactive state, use `StateFlow` collected with `collectAsState()`.
- For adaptive layouts, use `BoxWithConstraints` with breakpoints at 600 dp (medium) and 840 dp (expanded).

## Scaffolding with Claude Code

Use the [`/create-plugin`](../claude-code-skills.md#create-plugin) skill to scaffold a new plugin module from scratch — it creates the `build.gradle.kts`, base implementation class, and registers the module in `settings.gradle.kts`.
