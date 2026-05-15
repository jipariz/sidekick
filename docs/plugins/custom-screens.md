# Custom Screens

Wrap any Composable as a first-class debug screen in the Sidekick panel. Use it for feature-flag toggles, environment switchers, internal QA dashboards — anything you'd otherwise jury-rig with a hidden gesture.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)
![Web JS](https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black)
![Web Wasm](https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white)

## Features

- **Any Composable** — render whatever you want; the slot is unrestricted.
- **Full DI access** — the `content` Composable executes inside the host's composition tree, so Koin / Hilt / `CompositionLocal` providers all work as usual.
- **Many instances** — create as many `CustomScreenPlugin` cards as you need; each appears in the panel grid.
- **No state plumbing** — your Composable owns its state via the standard `remember` / `ViewModel` patterns.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:custom-screens:api` | `CustomScreenPlugin` wrapper (the only thing this plugin needs). |

## Setup

### 1. Add dependencies

```kotlin
commonMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:0.1.0"))
    implementation("dev.parez.sidekick:custom-screens")
}
```

### 2. Wire into Sidekick

```kotlin
val featureFlagsScreen = remember {
    CustomScreenPlugin(
        id    = "com.myapp.feature-flags",
        title = "Feature Flags",
        icon  = Icons.Default.Flag,
    ) {
        // any Composable — DI, ViewModels, CompositionLocals all work here
        FeatureFlagsScreen()
    }
}

Sidekick(
    plugins = listOf(featureFlagsScreen),
    actions = {
        IconButton(onClick = { sidekickVisible = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    },
)
```

Create as many instances as you need and pass them all to `Sidekick`. Because `content` executes inside the host app's composition tree, DI frameworks (Koin, Hilt, custom `CompositionLocal`s) work without extra wiring.

## Configuration

| Parameter | Description |
|---|---|
| `id` | Unique identifier across all plugins. Use kebab-case or reverse-domain (e.g. `"com.myapp.flags"`). |
| `title` | Label shown in the plugin grid card and screen header. |
| `icon` | `ImageVector` shown in the plugin grid card. |
| `content` | Composable rendered when the user opens this screen. Fills the plugin panel. |

## UI

When the user taps the card in the plugin grid, your `content` Composable fills the full plugin panel area. The active `MaterialTheme` is your app's (or Sidekick's, depending on `useSidekickTheme`). Use `Modifier.fillMaxSize()` on your root.

## Advanced

### Many screens at once

A single `CustomScreenPlugin` represents one card. If you have multiple debug views, create one plugin per view:

```kotlin
val plugins = remember {
    listOf(
        CustomScreenPlugin("com.myapp.flags",       "Feature Flags",  Icons.Default.Flag)        { FeatureFlagsScreen() },
        CustomScreenPlugin("com.myapp.env",         "Environment",    Icons.Default.Public)      { EnvironmentScreen() },
        CustomScreenPlugin("com.myapp.build-info",  "Build Info",     Icons.Default.Info)        { BuildInfoScreen() },
    )
}
```

### Scaffolding with Claude Code

Use the [`/create-plugin`](../claude-code-skills.md#create-plugin) skill to scaffold a full `SidekickPlugin` module (not just a `CustomScreenPlugin` wrapper) — useful when your debug screen has enough complexity to warrant its own Gradle module.

## See also

- [Creating a Custom Plugin](custom-plugin.md) — when you want a full module with its own DI scope.
- [Preferences](preferences.md) — for typed settings, prefer the dedicated plugin.
