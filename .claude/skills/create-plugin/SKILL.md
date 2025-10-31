---
name: create-plugin
description: Scaffold a new Sidekick plugin. Creates the plugins/<name>/api module, registers it in settings.gradle.kts, and generates a base SidekickPlugin implementation. Use when the user says "create a plugin", "add a plugin", or "new plugin".
argument-hint: "<plugin-name>"
allowed-tools: Read Write Edit Bash Glob Grep
---

Scaffold a new Sidekick plugin for this project. The plugin name is: **$ARGUMENTS**

## Naming conventions

Derive all names from `$ARGUMENTS` (expected in kebab-case, e.g. `network-inspector`):

| Form | Derivation | Example |
|---|---|---|
| kebab-case | `$ARGUMENTS` as-is | `network-inspector` |
| package segment | lowercase, hyphens removed | `networkinspector` |
| PascalCase prefix | each word capitalised | `NetworkInspector` |
| camelCase accessor | first word lowercase | `networkInspector` |

If `$ARGUMENTS` is empty, ask the user for the plugin name before proceeding.

## Steps

### 1. Read current state
- Read `settings.gradle.kts` to find where to insert the new `include` line.
- Read `plugins/preferences/api/build.gradle.kts` as a reference for the correct build file shape.

### 2. Create `plugins/<kebab>/api/build.gradle.kts`

```kotlin
plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.<package-segment>"
}
```

### 3. Create the base plugin class

Path: `plugins/<kebab>/api/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/<Pascal>Plugin.kt`

```kotlin
package dev.parez.sidekick.<package-segment>

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickPlugin

class <Pascal>Plugin : SidekickPlugin {
    override val id: String = "<kebab>"
    override val title: String = "<Human readable title>"
    override val icon: ImageVector = Icons.Default.Settings

    @Composable
    override fun Content() {
        // TODO: implement plugin UI
    }
}
```

Use `Icons.Default.Settings` as a placeholder icon. Pick a more appropriate icon if the plugin's purpose makes one obvious (e.g. `Icons.Default.NetworkCheck` for a network plugin).

### 4. Register in `settings.gradle.kts`

Add this line after the last `include(":plugins:…")` line:

```
include(":plugins:<kebab>:api")
```

### 5. Verify

Run `./gradlew projects` and confirm `:plugins:<kebab>:api` appears in the output.

### 6. Report to the user

Tell the user:
- Which files were created
- The Gradle module path (`:plugins:<kebab>:api`)
- How to depend on it: `implementation(projects.plugins.<camelCase>.api)` in `demo-app/build.gradle.kts`
- That they can swap the placeholder icon and implement `Content()` to build out the plugin UI

## Rules
- Do NOT create a KSP submodule unless the user explicitly asks for annotation-based code generation.
- Do NOT modify the convention plugin (`SidekickKmpLibraryPlugin`) or any existing module.
- Do NOT add the new plugin to `demo-app` automatically — just tell the user how to do it.
- The Android namespace must follow the pattern `dev.parez.sidekick.<package-segment>` (no hyphens, all lowercase).
