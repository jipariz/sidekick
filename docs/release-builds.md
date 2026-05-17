# Release Builds

Sidekick strips itself from production builds via dedicated noop modules. There are two layers to swap:

1. **`core:runtime` → `core:noop`** — replaces the `Sidekick()` composable with an empty body. No panel, no overlay UI.
2. **`network-monitor:{plugin,ktor}` → `network-monitor:noop`** and **`log-monitor:{plugin,kermit}` → `log-monitor:noop`** — replaces the recording side with empty stubs. No SQLDelight database opens, no HTTP/log entries are persisted, every `recordX` / `install` / `log` call is a no-op.

The public ABI is identical across `runtime`/`noop` and across each `{api,plugin,ktor|kermit}`/`noop` family — same FQNs, same signatures. Consumer code compiles unchanged in both variants.

## Android (single-module app)

`debugImplementation` / `releaseImplementation` are Android Gradle Plugin configurations. Wire the swap once in your Android app module's `dependencies { }` block; the BOM resolves the per-family versions:

```kotlin
// build.gradle.kts (Android app module)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(platform("dev.parez.sidekick:bom:2026.05.16"))
            // `compileOnly` keeps the real plugin jars off the Android release
            // runtime classpath, where they would collide with the noop variant.
            compileOnly("dev.parez.sidekick:network-monitor-plugin")
            compileOnly("dev.parez.sidekick:network-monitor-ktor")
            compileOnly("dev.parez.sidekick:log-monitor-plugin")
            compileOnly("dev.parez.sidekick:log-monitor-kermit")
        }
    }
}

dependencies {
    debugImplementation("dev.parez.sidekick:runtime")
    releaseImplementation("dev.parez.sidekick:noop")

    debugImplementation("dev.parez.sidekick:network-monitor-plugin")
    debugImplementation("dev.parez.sidekick:network-monitor-ktor")
    releaseImplementation("dev.parez.sidekick:network-monitor-noop")
    debugImplementation("dev.parez.sidekick:log-monitor-plugin")
    debugImplementation("dev.parez.sidekick:log-monitor-kermit")
    releaseImplementation("dev.parez.sidekick:log-monitor-noop")
}
```

## Android (multi-module KMP app)

If the module that *calls* `Sidekick()` is a KMP library, you can't put the variant-specific deps there — `debugImplementation` is an AGP concept that doesn't apply to KMP source sets. Split the wiring across two modules: the library compiles against the `compileOnly` types, the Android app module provides the per-variant impl. The full pattern is in [Installation › Multi-module KMP app](installation.md#multi-module-kmp-app).

## Non-Android targets (iOS / Desktop JVM / JS / Wasm)

> **`debugImplementation` / `releaseImplementation` only work on Android.** AGP owns those configurations; the other KMP targets have no equivalent build-type split, so **the consumer is responsible for picking the right module per build**.

The recommended pattern is a property-gated swap in each leaf source set. Run prod builds with `-Psidekick.noop=true`; dev builds use the default (real modules):

```kotlin
val sidekickNoop = (findProperty("sidekick.noop") as? String).toBoolean()

jvmMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.05.16"))
    if (sidekickNoop) {
        implementation("dev.parez.sidekick:noop")
        implementation("dev.parez.sidekick:network-monitor-noop")
        implementation("dev.parez.sidekick:log-monitor-noop")
    } else {
        implementation("dev.parez.sidekick:runtime")
        implementation("dev.parez.sidekick:network-monitor-plugin")
        implementation("dev.parez.sidekick:network-monitor-ktor")
        implementation("dev.parez.sidekick:log-monitor-plugin")
        implementation("dev.parez.sidekick:log-monitor-kermit")
    }
}
```

Mirror the same `if (sidekickNoop) { … } else { … }` block in `iosMain.dependencies`, `jsMain.dependencies`, and `wasmJsMain.dependencies`. Then:

```bash
./gradlew :app:packageDmg            # dev build (real plugins)
./gradlew :app:packageDmg -Psidekick.noop=true   # prod build (noop)
```

Alternative approaches (use what fits your CI):

- **Hand-rolled swap.** Edit the leaf source set's dependencies before cutting a release. Simplest, no Gradle property logic needed.
- **Runtime opt-out.** Don't call `install(NetworkMonitorKtor)` / don't register `LogMonitorLogWriter` when a feature flag is off. Works cross-platform without changing build config, but the SQLDelight DB still initialises at construction time (`NetworkMonitorPlugin(...)` runs `store.init()`).

## What gets stripped in noop

| Module | Stripped by `core:noop`? | Stripped by `{network,log}-monitor:noop`? |
|---|---|---|
| `Sidekick()` composable | ✅ Empty body. | — |
| Plugin host UI (list / detail navigation, theming) | ✅ Not present. | — |
| `NetworkMonitorPlugin` / `LogMonitorPlugin` constructors | ❌ Still constructed. | ✅ Empty `init {}` — no `store.init()`, no DB open. |
| `NetworkMonitorKtor` Ktor `ClientPlugin` | ❌ Still registers hooks. | ✅ Registers no `on(...)` hooks. |
| `LogMonitorLogWriter` | ❌ Still records. | ✅ `log()` discards entries. |
| SQLDelight `NetworkMonitorDatabase` / `LogMonitorDatabase` classes | ❌ Generated and dexed. | ✅ Absent from the binary entirely. |

For Android release builds with both swaps wired, `NetworkMonitorDatabase` and `LogMonitorDatabase` symbols are absent from the merged DEX. (Verified by inspecting the demo-app's release APK: 0 references in release vs 20 in debug for each.)

## Verifying the swap

Confirm release builds are clean by greping the merged DEX:

```bash
./gradlew :app:assembleRelease
cd app/build/outputs/apk/release
mkdir -p dex && unzip -q app-release-unsigned.apk 'classes*.dex' -d dex
for f in dex/*.dex; do strings "$f" | grep -c "NetworkMonitorDatabase"; done | paste -sd+ - | bc
# expected: 0
```

If you need to keep the real modules on the runtime classpath (e.g. for tests) but still skip recording, see the conditional-install recipes in [Network Monitor › Conditional install](plugins/network-monitor.md#conditional-install-in-release-builds) and [Log Monitor › Conditional bridge](plugins/log-monitor.md#conditional-bridge-in-release-builds).
