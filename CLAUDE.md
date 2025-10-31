# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sidekick is a **Kotlin Multiplatform** app using **Compose Multiplatform**, targeting Android, iOS, Desktop (JVM), Web (JS), and Web (Wasm). The package namespace is `dev.parez.sidekick`.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug

# Desktop (JVM) - run directly
./gradlew :composeApp:run

# Web (Wasm - modern browsers, faster)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web (JS - older browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run all tests
./gradlew :composeApp:allTests

# Run a specific test class
./gradlew :composeApp:jvmTest --tests "dev.parez.sidekick.GreetingTest"
```

iOS is built via Xcode (open `iosApp/` in Xcode) or through the IDE run configuration.

## Architecture

All shared UI and business logic lives in `composeApp/src/`:

- **`commonMain/`** — shared Kotlin/Compose code for all platforms. Platform-specific behavior is exposed via `expect fun getPlatform(): Platform` (defined in `Platform.kt`).
- **`androidMain/`**, **`iosMain/`**, **`jvmMain/`**, **`jsMain/`**, **`wasmJsMain/`**, **`webMain/`** — `actual` implementations of `expect` declarations for each platform.

The entry point for each platform:
- Android: `MainActivity` (`androidMain`)
- iOS: `MainViewController` (`iosMain`) — called from Swift in `iosApp/`
- Desktop: `main()` in `jvmMain/main.kt` — uses `application {}` window DSL
- Web: `main()` in `webMain/main.kt` — uses `ComposeViewport`

The root composable `App()` in `commonMain/App.kt` is shared across all targets.

## Dependency Management

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Always use `libs.*` accessors in `build.gradle.kts` files rather than hardcoded version strings. Key versions: Kotlin 2.2.21, Compose Multiplatform 1.9.2, AGP 8.13.0.

Gradle configuration cache and build caching are both enabled (`gradle.properties`). The JVM heap for the Gradle daemon is 4 GB and for the Kotlin daemon is 3 GB.
