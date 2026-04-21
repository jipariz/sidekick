# Custom Screens

`CustomScreenPlugin` wraps any Composable as a first-class debug screen in the Sidekick overlay. Each instance appears as its own card in the plugin grid.

## Setup

```kotlin
commonMain.dependencies {
    implementation(projects.plugins.customScreens.api)
}
```

## Usage

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
    onClose = { sidekickVisible = false },
)
```

Create as many instances as you need and pass them all to `Sidekick`. Because `content` executes inside the host app's composition tree, DI frameworks (Koin, Hilt, custom `CompositionLocal`s) work without any extra wiring.

## Parameters

| Parameter | Description |
|-----------|-------------|
| `id` | Unique identifier for this screen. Use kebab-case or reverse-domain (e.g. `"com.myapp.flags"`). |
| `title` | Label shown in the plugin grid card and screen header. |
| `icon` | Icon shown in the plugin grid card. |
| `content` | Composable rendered when the user opens this screen. |
