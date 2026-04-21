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

Ask the user one question before scaffolding: **"Does this plugin need managed singletons or ViewModels (Koin DI)?"**
- If **yes** (or the plugin has a store/database/background work), follow the [Stateful plugin](#stateful-plugin-with-koin) path.
- If **no** (simple stateless UI), follow the [Simple plugin](#simple-plugin) path.

---

## Simple plugin

A stateless plugin — only `api` module needed.

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
    override fun Content(navigateBackToList: () -> Unit) {
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

---

## Stateful plugin (with Koin)

Mirrors the `network-monitor` architecture: `api` module owns the data store + isolated Koin context; `plugin` module owns the Compose UI + ViewModel.

### 1. Read current state
- Read `settings.gradle.kts` to find insertion points.
- Read `plugins/network-monitor/api/build.gradle.kts` and `plugins/network-monitor/plugin/build.gradle.kts` as references.

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
            api(libs.koin.core)   // api so plugin module inherits transitively
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.<package-segment>"
}
```

### 3. Create the store

Path: `plugins/<kebab>/api/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/<Pascal>Store.kt`

```kotlin
package dev.parez.sidekick.<package-segment>

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class <Pascal>Store(private val scope: CoroutineScope) {
    private val _items = MutableStateFlow<List<String>>(emptyList())
    val items: Flow<List<String>> = _items.asStateFlow()

    suspend fun clear() {
        _items.value = emptyList()
    }
}
```

### 4. Create the isolated Koin context

Path: `plugins/<kebab>/api/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/di/<Pascal>KoinContext.kt`

```kotlin
package dev.parez.sidekick.<package-segment>.di

import dev.parez.sidekick.<package-segment>.<Pascal>Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * Isolated Koin context for the <Pascal> plugin.
 * Never conflicts with the host app's Koin setup.
 */
object <Pascal>KoinContext {
    val koinApp: KoinApplication = koinApplication {
        modules(<camel>CoreModule)
    }
    val koin get() = koinApp.koin

    private var viewModelModuleLoaded = false

    /** Returns the singleton [<Pascal>Store]. Used as the default in config classes
     *  so sibling modules don't need a direct Koin dependency. */
    fun getDefaultStore(): <Pascal>Store = koin.get()

    /** Loads the ViewModel module from `<kebab>:plugin` exactly once. */
    fun loadViewModelModule(module: Module) {
        if (!viewModelModuleLoaded) {
            viewModelModuleLoaded = true
            koinApp.koin.loadModules(listOf(module))
        }
    }
}

internal val <camel>CoreModule = module {
    single { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    singleOf(::<Pascal>Store)
}
```

### 5. Create `plugins/<kebab>/plugin/build.gradle.kts`

```kotlin
plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.<camelCase>.api)
            api(projects.core.pluginApi)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.<package-segment>.plugin"
}
```

### 6. Create the ViewModel

Path: `plugins/<kebab>/plugin/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/<Pascal>ViewModel.kt`

```kotlin
package dev.parez.sidekick.<package-segment>

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class <Pascal>ViewModel(
    private val store: <Pascal>Store,
) : ViewModel() {

    val items: StateFlow<List<String>> = store.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun clear() {
        viewModelScope.launch { store.clear() }
    }
}
```

### 7. Create the Koin module for the ViewModel

Path: `plugins/<kebab>/plugin/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/di/<Pascal>Module.kt`

```kotlin
package dev.parez.sidekick.<package-segment>.di

import dev.parez.sidekick.<package-segment>.<Pascal>ViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val <camel>ViewModelModule = module {
    viewModelOf(::<Pascal>ViewModel)
}
```

### 8. Create the plugin class

Path: `plugins/<kebab>/plugin/src/commonMain/kotlin/dev/parez/sidekick/<package-segment>/<Pascal>Plugin.kt`

```kotlin
package dev.parez.sidekick.<package-segment>

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.parez.sidekick.<package-segment>.di.<Pascal>KoinContext
import dev.parez.sidekick.<package-segment>.di.<camel>ViewModelModule
import dev.parez.sidekick.plugin.SidekickPlugin
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

class <Pascal>Plugin : SidekickPlugin {

    init {
        <Pascal>KoinContext.loadViewModelModule(<camel>ViewModelModule)
    }

    override val id: String = "<kebab>"
    override val title: String = "<Human readable title>"
    override val icon: ImageVector = Icons.Default.Settings

    @Composable
    override fun Content(navigateBackToList: () -> Unit) {
        KoinIsolatedContext(context = <Pascal>KoinContext.koinApp) {
            val viewModel: <Pascal>ViewModel = koinViewModel()
            val items by viewModel.items.collectAsStateWithLifecycle()

            // TODO: replace with actual UI
            <Pascal>Content(
                items = items,
                onClear = viewModel::clear,
                onBack = navigateBackToList,
            )
        }
    }
}
```

### 9. Register both modules in `settings.gradle.kts`

```
include(":plugins:<kebab>:api")
include(":plugins:<kebab>:plugin")
```

### 10. Verify

Run `./gradlew projects` and confirm both modules appear.

### 11. Report to the user

Tell the user:
- Which files were created (`api` + `plugin` modules)
- Gradle paths (`:plugins:<kebab>:api` and `:plugins:<kebab>:plugin`)
- Consumers should add `implementation(projects.plugins.<camelCase>.plugin)` to their `build.gradle.kts`
- `<Pascal>Store` is the singleton managed by Koin — inject it anywhere in the plugin via `<Pascal>KoinContext.koin.get()`
- They should replace the placeholder `<Pascal>Content` composable with real UI

---

## Rules
- Do NOT create a KSP submodule unless the user explicitly asks for annotation-based code generation.
- Do NOT modify the convention plugin (`SidekickKmpLibraryPlugin`) or any existing module.
- Do NOT add the new plugin to `demo-app` automatically — just tell the user how to do it.
- The Android namespace must follow the pattern `dev.parez.sidekick.<package-segment>` (no hyphens, all lowercase).
- For the stateful path, `koin-core` must be `api` (not `implementation`) in the `api` module so sibling modules (e.g. a Ktor interceptor) can reach `<Pascal>KoinContext` without adding Koin themselves.
