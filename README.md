<h1 align="center">Sidekick</h1>

<p align="center">
  <strong>A Kotlin Multiplatform debug overlay SDK.</strong><br/>
  Network inspector, log viewer, typed preferences, and custom screens — in one floating panel your app embeds during development.
</p>

<p align="center">
  <img alt="Android"      src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white">
  <img alt="iOS"          src="https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white">
  <img alt="Desktop (JVM)" src="https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white">
  <img alt="Web (JS)"     src="https://img.shields.io/badge/Web_(JS)-F7DF1E?logo=javascript&logoColor=black">
  <img alt="Web (Wasm)"   src="https://img.shields.io/badge/Web_(Wasm)-654FF0?logo=webassembly&logoColor=white">
</p>

<p align="center">
  <img alt="License: Apache 2.0" src="https://img.shields.io/badge/license-Apache_2.0-blue">
  <img alt="Kotlin 2.3.20"       src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose Multiplatform 1.10.3" src="https://img.shields.io/badge/Compose_Multiplatform-1.10.3-4285F4">
  <img alt="Maven Central" src="https://img.shields.io/maven-central/v/dev.parez.sidekick/bom">
</p>

<p align="center">
  <a href="https://jipariz.github.io/sidekick/"><strong>Documentation</strong></a> ·
  <a href="https://jipariz.github.io/sidekick/demo/"><strong>Live demo</strong></a> ·
  <a href="docs/installation.md">Installation</a> ·
  <a href="docs/quick-start.md">Quick start</a>
</p>

---

## ✨ Why Sidekick

- 🌐 **Inspect HTTP traffic without a proxy** — built-in Ktor integration; any other client plugs in via a low-level store API.
- 🪵 **View logs without ADB** — Kermit bridge ships out of the box; Timber and friends drop in via a 4-line `LogCollector`.
- 🎚️ **Flip feature flags from the panel** — annotate a class, KSP generates the DataStore wiring and a ready-to-use UI.
- 🧩 **Wrap any Composable as a debug screen** — internal QA dashboards, environment switchers, build-info pages.
- ⚡ **Zero release-build cost** — `core:noop` swaps the overlay for a passthrough composable; release binaries don't ship one byte of Sidekick UI code.
- 🖼️ **Compose Multiplatform** — one codebase, five targets: Android, iOS, Desktop (JVM), Web (JS), Web (Wasm).
- 🎨 **Theme-aware** — applies its own light/dark palette by default, or inherits your `MaterialTheme` with one flag.

## 🧱 Built-in plugins

| Plugin | What it does | Platforms |
|---|---|---|
| [**Network Monitor**](docs/plugins/network-monitor.md) | Captures every HTTP request / response. Ktor built-in; OkHttp and others via `NetworkMonitorStore`. | ![A](https://img.shields.io/badge/-Android-3DDC84) ![i](https://img.shields.io/badge/-iOS-000) ![J](https://img.shields.io/badge/-JVM-4E8EE9) ![J](https://img.shields.io/badge/-JS-F7DF1E) ![W](https://img.shields.io/badge/-Wasm-654FF0) |
| [**Log Monitor**](docs/plugins/log-monitor.md) | Color-coded log feed with level chips and search. Kermit bridge built-in. | ![A](https://img.shields.io/badge/-Android-3DDC84) ![i](https://img.shields.io/badge/-iOS-000) ![J](https://img.shields.io/badge/-JVM-4E8EE9) ![J](https://img.shields.io/badge/-JS-F7DF1E) ![W](https://img.shields.io/badge/-Wasm-654FF0) |
| [**Preferences**](docs/plugins/preferences.md) | Typed settings UI generated from `@Preference` annotations via KSP. | ![A](https://img.shields.io/badge/-Android-3DDC84) ![i](https://img.shields.io/badge/-iOS-000) ![J](https://img.shields.io/badge/-JVM-4E8EE9) ![J](https://img.shields.io/badge/-JS-F7DF1E) ![W](https://img.shields.io/badge/-Wasm-654FF0)¹ |
| [**Custom Screens**](docs/plugins/custom-screens.md) | Wrap any Composable as a debug card. Full DI access. | ![A](https://img.shields.io/badge/-Android-3DDC84) ![i](https://img.shields.io/badge/-iOS-000) ![J](https://img.shields.io/badge/-JVM-4E8EE9) ![J](https://img.shields.io/badge/-JS-F7DF1E) ![W](https://img.shields.io/badge/-Wasm-654FF0) |
| [**Your plugin**](docs/plugins/custom-plugin.md) | Implement `SidekickPlugin` — full module, your own DI scope, anything goes. | depends on what you publish |

<sub>¹ Wasm uses in-memory preferences (DataStore has no Wasm driver) — values do not persist across reloads.</sub>

## 🚀 Quick install

Pin the **BOM** once; every plugin line is then version-agnostic. The BOM is calendar-versioned (`YYYY.MM.DD`); check the Maven Central badge above for the latest. The two variant-scoped lines (`debugImplementation` / `releaseImplementation`) take an explicit module version because Android Gradle Plugin variant configurations don't pick up the BOM by default.

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation("dev.parez.sidekick:runtime:0.1.0")
    releaseImplementation("dev.parez.sidekick:noop:0.1.0")  // no-op in release — zero cost
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // BOM pins every plugin module to its current family version.
            implementation(platform("dev.parez.sidekick:bom:2026.05.16"))
            implementation("dev.parez.sidekick:network-monitor-plugin")
            implementation("dev.parez.sidekick:network-monitor-ktor")
            implementation("dev.parez.sidekick:log-monitor-plugin")
            implementation("dev.parez.sidekick:log-monitor-kermit")
            implementation("dev.parez.sidekick:preferences")
            implementation("dev.parez.sidekick:custom-screens")
        }
    }
}
```

<details>
<summary><strong>Using a Gradle version catalog?</strong></summary>

Drop this in `gradle/libs.versions.toml`:

```toml
[versions]
# BOM is calendar-versioned — bump when you want to track the latest published release.
sidekick-bom = "2026.05.16"
# Core family (runtime, noop). Needed directly because the BOM doesn't propagate
# to Android `debugImplementation` / `releaseImplementation` configurations.
sidekick-core = "0.1.0"

[libraries]
sidekick-bom     = { module = "dev.parez.sidekick:bom",     version.ref = "sidekick-bom" }
sidekick-runtime = { module = "dev.parez.sidekick:runtime", version.ref = "sidekick-core" }
sidekick-noop    = { module = "dev.parez.sidekick:noop",    version.ref = "sidekick-core" }
# Plugin modules — version is pulled from the BOM at use site.
sidekick-network-monitor-plugin = { module = "dev.parez.sidekick:network-monitor-plugin" }
sidekick-network-monitor-ktor   = { module = "dev.parez.sidekick:network-monitor-ktor" }
sidekick-log-monitor-plugin     = { module = "dev.parez.sidekick:log-monitor-plugin" }
sidekick-log-monitor-kermit     = { module = "dev.parez.sidekick:log-monitor-kermit" }
sidekick-preferences            = { module = "dev.parez.sidekick:preferences" }
sidekick-custom-screens         = { module = "dev.parez.sidekick:custom-screens" }

[plugins]
# Gradle plugins resolve via the plugin DSL, not via the BOM.
sidekick-preferences = { id = "dev.parez.sidekick.preferences", version = "0.1.0" }
```

Then in `build.gradle.kts`: `debugImplementation(libs.sidekick.runtime)`, `implementation(platform(libs.sidekick.bom))`, `implementation(libs.sidekick.network.monitor.plugin)`, `alias(libs.plugins.sidekick.preferences)`, etc.

</details>

<details>
<summary><strong>Per-platform notes & KSP setup</strong></summary>

- **Desktop (JVM)** — `debugImplementation` is Android-only; add `jvmMain.dependencies { implementation("dev.parez.sidekick:runtime:0.1.0") }` and swap to `dev.parez.sidekick:noop` for production builds yourself.
- **KSP for Preferences** — easiest path: apply the `dev.parez.sidekick.preferences` Gradle plugin, which wires the KSP processor, the generated-sources directory, and the task dependencies for you. Full snippet (and a manual alternative) in [docs/installation.md](docs/installation.md).
- **Android `ContentProvider`** — `dev.parez.sidekick:plugin-api` ships a `SidekickInitializer` that auto-initializes the library context. No manual call required.

</details>

## 🧰 Wire it into your app

The client app owns the FAB and visibility; `Sidekick` only renders the panel. The trailing `actions` slot is where you place the close button.

```kotlin
@Composable
fun App() {
    val networkPlugin = remember { NetworkMonitorPlugin() }
    val logPlugin     = remember { LogMonitorPlugin() }
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
                    actions = {
                        IconButton(onClick = { sidekickVisible = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                )
            }
        }
    }
}
```

Use any trigger — a shake gesture, a hidden tap zone, a build-type check. Sidekick is just a composable.

## 📚 Documentation

| Topic | |
|---|---|
| [Installation](docs/installation.md) | Per-platform notes, KSP setup. |
| [Quick start](docs/quick-start.md) | Wire-up snippet, header customization, `appInfo`. |
| [Release builds](docs/release-builds.md) | Swap `core:runtime` → `core:noop`. Zero overhead. |
| [Theming](docs/theming.md) | Use Sidekick's theme or inherit yours. HTTP badge colors. |
| [Network Monitor](docs/plugins/network-monitor.md) | Ktor integration, OkHttp recipe, sanitization, retention. |
| [Log Monitor](docs/plugins/log-monitor.md) | Kermit bridge, Timber recipe, custom `LogCollector`. |
| [Preferences](docs/plugins/preferences.md) | `@Preference` annotations, KSP setup, DataStore migration. |
| [Custom Screens](docs/plugins/custom-screens.md) | Wrap any Composable as a debug card. |
| [Creating a Custom Plugin](docs/plugins/custom-plugin.md) | Implement `SidekickPlugin` end-to-end. |

## 🤝 Contributing

Contributions welcome. The project layout, build conventions, and the architecture decisions behind the plugin system are documented in [`CLAUDE.md`](CLAUDE.md). Run all tests with:

```bash
./gradlew allTests
```

Publish snapshots locally with:

```bash
./gradlew publishToMavenLocal --no-configuration-cache
```

## 📄 License

Apache 2.0 — see [LICENSE](LICENSE).

## 🙏 Credits

The demo app uses data from [PokéAPI](https://pokeapi.co) — a free, open RESTful Pokémon API. See [pokeapi.co/about](https://pokeapi.co/about) for license and usage details.
