# Installation

Sidekick is a multi-module library. Add the modules you need as dependencies in your app's `build.gradle.kts`.

## Core

Every app needs the core runtime (debug builds) and the no-op stub (release builds):

```kotlin
// build.gradle.kts (Android)
dependencies {
    debugImplementation(projects.core.runtime)
    releaseImplementation(projects.core.noop)
}
```

!!! info "`core:noop`"
    The no-op module replaces `SidekickShell` with a passthrough composable that simply calls `content()`. Zero overhead — Sidekick is completely absent from release builds.

For **Desktop (JVM)**, add both explicitly since Gradle's `debugImplementation` is Android-only:

```kotlin
jvmMain.dependencies {
    implementation(projects.core.runtime)
}
```

## Plugins

Add the plugins you want to use:

```kotlin
commonMain.dependencies {
    // Network monitor
    implementation(projects.plugins.networkMonitor.plugin)
    implementation(projects.plugins.networkMonitor.ktor) // Ktor integration

    // Log monitor
    implementation(projects.plugins.logMonitor.plugin)
    implementation(projects.plugins.logMonitor.kermit)   // Kermit bridge (optional)

    // Preferences
    implementation(projects.plugins.preferences.api)

    // Custom screens
    implementation(projects.plugins.customScreens.api)
}
```

## KSP (Preferences code generator)

The Preferences plugin ships a KSP processor that generates boilerplate from annotations. Apply the KSP plugin and register the processor:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", projects.plugins.preferences.ksp)
}

// All compile and KSP tasks must wait for the common-metadata KSP pass
tasks.matching { task ->
    task.name != "kspCommonMainKotlinMetadata" &&
        (task.name.startsWith("compile") && task.name.contains("Kotlin") ||
            task.name.startsWith("ksp"))
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

// Disable build caching for the KSP task (source dir registration is unreliable in cache)
tasks.matching { it.name == "kspCommonMainKotlinMetadata" }.configureEach {
    outputs.cacheIf { false }
    val outDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
    outputs.upToDateWhen { outDir.get().asFile.exists() }
}
```

!!! tip "Automated setup"
    Use the [`/setup-sidekick`](claude-code-skills.md#setup-sidekick) Claude Code skill to handle all of this automatically, including KSP wiring and plugin selection.
