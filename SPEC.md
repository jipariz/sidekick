Sidekick SDK: Master Architecture Document (v2)

1. Core Vision & Principles

Product Vision: To provide developers with a "sidekick" for their Compose Multiplatform apps (Android, iOS, Desktop, and Wasm), offering a pluggable, secure, and production-ready set of debug tools.

Core Principles:

Zero Footprint (Default): By default, the entire Sidekick SDK must be 100% stripped from a release build. It should add zero bytes and zero methods (the "noop" configuration).

Configurable Production Access (Opt-In): Developers must have a clear, intentional opt-in path to include Sidekick in a release build.

Secure by Default: When in production, Sidekick must be dormant and only activatable by a cryptographically trusted companion app (on mobile) or a secure developer-triggered action (on Wasm). In-app gestures (like tapping) must be disabled.

Decoupled & Production-Safe: SDK features that are wrappers (like SidekickPreferences) must be usable in release builds, even when the UI is stripped. This is the core "API vs. UI" separation.

Extensible: The plugin system is the core. Adding a new tool should be as simple as implementing one interface (SidekickPlugin).

2. The "Facade" Design Pattern

This is the most important concept to understand. The developer's app code (e.g., MyAppSidekick.kt) will call Sidekick.register(). How does this work in all build types?

It works because we provide three different "facade" modules (sidekick-debug, sidekick-production, sidekick-noop) that all expose the exact same public API (a Sidekick object and a SidekickShell composable). The developer uses Gradle's debugImplementation and releaseImplementation (and platform-specific variants like wasmJsDebugImplementation) to "swap" which facade is compiled into their app.

Developer's Code (in debug source set): Sidekick.register(myPlugins)

Debug Build: This call resolves to sidekick-debug. This facade is a real implementation that depends on sidekick-core (the UI) and registers gestures + companion/activation listeners.

Release Build (Config B): This call resolves to sidekick-production. This facade is also a real implementation that depends on sidekick-core, but it only registers secure activation listeners.

Release Build (Config A): This call resolves to sidekick-noop. This facade is a stub. The register() function is empty. The SidekickShell is an empty Composable. The compiler and R8/ProGuard/Wasm Optimizer will strip it all away.

3. Module-by-Module Breakdown

3.1. sidekick-plugin-api

Purpose: Defines the contract for all plugins.

Build Type: Compile-time only. Included by all plugin UIs and SDK facades.

Dependencies: None.

Key File: SidekickPlugin.kt

/**
* The core interface for any Sidekick plugin.
* This is the "contract" for a tool's UI.
  */
  interface SidekickPlugin {
  /**
    * A unique machine-readable key.
    * e.g., "preferences", "network_logger", "my_app_feature_flags"
      */
      val id: String

  /**
    * The human-readable name for navigation.
    * e.g., "App Preferences", "Network Logger"
      */
      val name: String

  /**
    * The Composable content for this plugin's screen.
    * This function will be called by the core UI shell.
      */
      @Composable
      fun Content()
      }


3.2. sidekick-preferences-api

Purpose: The production-safe wrapper for DataStore.

Build Type: Production Safe. Included in all developer builds (implementation).

Dependencies: androidx.datastore:datastore-preferences

Key File: SidekickPreferences.kt

// This sealed class defines the "schema" of a preference
sealed class SidekickPref<T>(val key: Preferences.Key<T>, val default: T, val name: String) {
class BooleanPref(keyName: String, name: String, default: Boolean) : SidekickPref<Boolean>(booleanPreferencesKey(keyName), default, name)
class StringPref(keyName: String, name: String, default: String) : SidekickPref<String>(stringPreferencesKey(keyName), default, name)
class IntPref(keyName: String, name: String, default: Int) : SidekickPref<Int>(intPreferencesKey(keyName), default, name)
// ... etc. for float, long
}

/**
* A production-safe, type-safe wrapper for DataStore<Preferences>.
* This class can be used in your release app's ViewModels, etc.
* It has NO dependency on any Sidekick UI.
  */
  class SidekickPreferences(
  private val dataStore: DataStore<Preferences>,
  val definitions: List<SidekickPref<*>> // The "schema"
  ) {
  // Public API for the app to use
  fun <T> getFlow(pref: SidekickPref<T>): Flow<T> {
  return dataStore.data.map { it[pref.key] ?: pref.default }
  }

  suspend fun <T> setValue(pref: SidekickPref<T>, value: T) {
  dataStore.edit { it[pref.key] = value }
  }

  // Internal helper for the plugin
  fun getDefinitions(): List<SidekickPref<*>> = definitions
  }


3.3. sidekick-plugin-preferences

Purpose: The debug-only UI for the preferences wrapper.

Build Type: Debug Only. Included by the developer (debugImplementation or releaseImplementation for Config B).

Dependencies: sidekick-plugin-api, sidekick-preferences-api

Key File: PreferencesPlugin.kt

/**
* This is the UI plugin implementation.
* It knows nothing about how it's hosted. It just renders a UI
* based on the SidekickPreferences object it's given.
  */
  class PreferencesPlugin(
  private val prefs: SidekickPreferences
  ) : SidekickPlugin {

  override val id = "preferences"
  override val name = "App Preferences"

  @Composable
  override fun Content() {
  val scope = rememberCoroutineScope()
  // Get the "schema"
  val definitions = prefs.getDefinitions()

       LazyColumn {
           items(definitions) { pref ->
               // Use 'when' to render the correct UI for each type
               when (pref) {
                   is SidekickPref.BooleanPref -> {
                       val value by prefs.getFlow(pref).collectAsState(pref.default)
                       BooleanPreferenceRow(
                           name = pref.name,
                           value = value,
                           onToggle = { scope.launch { prefs.setValue(pref, it) } }
                       )
                   }
                   is SidekickPref.StringPref -> {
                       // ... UI for String ...
                   }
                   // ... etc.
               }
           }
       }
  }
  }


3.4. sidekick-core (Internal)

Purpose: The "engine" of the UI. Contains the shell, navigation, and plugin registry. Developers never touch this directly.

Build Type: Internal. Used by sidekick-debug and sidekick-production.

Dependencies: sidekick-plugin-api

Key Files:

PluginRegistry.kt: internal object PluginRegistry { val plugins = mutableStateListOf<SidekickPlugin>() }

InternalSidekickShell.kt:

@Composable
internal fun InternalSidekickShell(onDismissRequest: () -> Unit) {
val plugins = PluginRegistry.plugins
var selectedPlugin by remember { mutableStateOf<SidekickPlugin?>(plugins.firstOrNull()) }

    // Adaptive Navigation (e.g., ModalBottomSheetLayout on mobile)
    // ...
        // NavigationRail or BottomNavigation for plugin list
        // ...
        // Main content area
        selectedPlugin?.Content()
    // ...
}


3.5. The Facades (The Developer's API)

sidekick-debug

Purpose: The default debug facade. Enables all features.

Key Files:

Sidekick.kt:

object Sidekick {
fun register(plugins: List<SidekickPlugin>) {
PluginRegistry.plugins.addAll(plugins)
// 1. Register in-app gestures (e.g., shake on mobile, DOM listener on Wasm)
GestureListener.start()
// 2. Register companion/activation listeners
// (e.g., Intent/URL scheme on mobile, URL query param ?sidekick=true on Wasm)
ActivationListener.start()
}
}
@Composable
fun SidekickShell(onDismissRequest: () -> Unit) {
// Public composable delegates to the internal one
InternalSidekickShell(onDismissRequest)
}


sidekick-production (Opt-In)

Purpose: The opt-in release facade. Enables secure-only access.

Key Files:

Sidekick.kt:

object Sidekick {
fun register(plugins: List<SidekickPlugin>) {
PluginRegistry.plugins.addAll(plugins)
// 1. DO NOT register gestures
// 2. Register secure activation listeners ONLY
// (e.g., Intent/URL scheme on mobile).
// On Wasm, this does nothing, activation is manual (see 5.3).
ActivationListener.start()
}
}
@Composable
fun SidekickShell(onDismissRequest: () -> Unit) {
// Public composable delegates to the internal one
InternalSidekickShell(onDismissRequest)
}


sidekick-noop (Default)

Purpose: The default release facade. Disables and strips everything.

Key Files:

Sidekick.kt:

object Sidekick {
// This function is empty. It will be removed by R8/ProGuard.
fun register(plugins: List<SidekickPlugin>) { /* no-op */ }
}
@Composable
fun SidekickShell(onDismissRequest: () -> Unit) {
// This composable is empty.
/* no-op */
}


4. Developer Integration (The "How-To" Guide)

4.1. Gradle Setup (app/build.gradle.kts)

This is how the developer "chooses" their configuration.

Configuration A: Default (No Production Access)

// In app/build.gradle.kts
dependencies {
// 1. Production-safe API is always 'implementation'
implementation("com.sidekick:sidekick-preferences-api:1.0.0")

    // 2. The FULL debug facade for 'debug' builds (for all targets)
    debugImplementation("com.sidekick:sidekick-debug:1.0.0")
    debugImplementation("com.sidekick:sidekick-plugin-preferences:1.0.0")

    // 3. The NO-OP facade for 'release' builds (for all targets)
    releaseImplementation("com.sidekick:sidekick-noop:1.0.0")
}


Configuration B: Opt-In (Production Access via Companion/Secure)

// In app/build.gradle.kts
dependencies {
// 1. Production-safe API
implementation("com.sidekick:sidekick-preferences-api:1.0.0")

    // 2. The FULL debug facade for 'debug' builds
    debugImplementation("com.sidekick:sidekick-debug:1.0.0")
    debugImplementation("com.sidekick:sidekick-plugin-preferences:1.0.0")

    // 3. The PRODUCTION facade for 'release' builds
    releaseImplementation("com.sidekick:sidekick-production:1.0.0")
    // We must also include the plugins we want in production
    releaseImplementation("com.sidekick:sidekick-plugin-preferences:1.0.0")
}


4.2. Application Code (File Structure)

This shows where the developer's code lives.

src/commonMain/kotlin/com/myapp/MyAppLogic.kt

// This code is in commonMain. It exists in ALL builds.
object MyAppLogic {
// Get the DataStore instance (from KMP library like Multiplatform-Settings,
// which must have a Wasm-compatible storage backend like localStorage)
private val dataStore = createDataStore()

    // The wrapper is safe to initialize and use everywhere.
    val appPrefs = SidekickPreferences(
        dataStore = dataStore,
        definitions = listOf(
            SidekickPref.BooleanPref("is_dark_mode", "Dark Mode", false),
            SidekickPref.StringPref("auth_token", "Auth Token", "")
        )
    )
    
    // Your release app's ViewModels can use appPrefs.getFlow(...)
}


src/debug/kotlin/com/myapp/MyAppSidekick.kt

// This file ONLY exists in the 'debug' build.
object MyAppSidekick {
fun initialize() {
// 1. Create the plugin instances
val prefsPlugin = PreferencesPlugin(MyAppLogic.appPrefs)
val customPlugin = MyCustomAppPlugin() // Their own plugin

        // 2. Register them with the 'sidekick-debug' facade
        Sidekick.register(
            plugins = listOf(prefsPlugin, customPlugin)
        )
    }
}

// In your debug Composable (e.g., a hidden button)
// This calls the 'SidekickShell' from 'sidekick-debug'
@Composable
fun MyDebugScreen() {
var showSidekick by remember { mutableStateOf(false) }
// e.g., on 5-tap
// The debug facade's listeners (GestureListener, ActivationListener)
// will also toggle Sidekick's visibility internally.
if (showSidekick) {
SidekickShell(onDismissRequest = { showSidekick = false })
}
}


src/release/kotlin/com/myapp/MyAppSidekick.kt

// This file ONLY exists in the 'release' build.
object MyAppSidekick {
fun initialize() {
// For Config A (noop), this calls the empty Sidekick.register()
// For Config B (production), this calls the 'sidekick-production'
// facade, which is exactly what we want.

        // 1. Create the plugin instances
        val prefsPlugin = PreferencesPlugin(MyAppLogic.appPrefs)
        val customPlugin = MyCustomAppPlugin()
        
        // 2. Register them.
        Sidekick.register(
            plugins = listOf(prefsPlugin, customPlugin)
        )
    }
}
// Note: In release builds, there is no in-app gesture.
// The SidekickShell is ONLY launched by the companion app (mobile)
// or developer console (Wasm).


Note: For Config A, R8/ProGuard will see that Sidekick.register is empty and will likely strip this entire file and all the plugins, achieving the "zero footprint" goal.

5. Activation & Handshake (Detailed)

This is the platform-specific magic. The ActivationListener (CompanionListener in previous doc) in sidekick-debug and sidekick-production will provide helpers for this.

5.1. Android

Host App (SDK Side): The sidekick-core module will provide an Activity.

SidekickActivity.kt: internal class SidekickActivity : ComponentActivity() { ... }

This Activity's only job is to host the SidekickShell composable.

Host App (Developer Side): The developer must add this to their manifest.

File: src/debug/AndroidManifest.xml (and src/release/AndroidManifest.xml for Config B)

Code:

<manifest ...>
<application ...>
<!-- This Activity is provided by the SDK -->
<activity
android:name="com.sidekick.core.SidekickActivity"
android:exported="true"
android:theme="@style/Theme.AppCompat.Translucent">
<intent-filter>
<!-- This action is what the companion app will fire -->
<action android:name="com.sidekick.LAUNCH" />
<category android:name="android.intent.category.DEFAULT" />
</intent-filter>
</activity>
</application>
</manifest>


Companion App (SDK Side): The sidekick-companion-app will...

Get a list of apps: val apps = packageManager.getInstalledApplications(...)

Filter for apps that have the com.sidekick.LAUNCH intent: packageManager.queryIntentActivities(...)

For each app found, check signatures: packageManager.checkSignatures(myPackageName, hostPackageName)

If SIGNATURE_MATCH, show a button for that app.

On click, fire the intent: val intent = Intent("com.sidekick.LAUNCH").setPackage(hostPackageName)

startActivity(intent)

5.2. iOS

Host App (SDK Side): The sidekick-core module will provide a helper.

SidekickHelper.swift: public func handleUrl(url: URL) -> Bool { ... }

Host App (Developer Side):

Register the URL scheme.

File: Info.plist (in debug and release for Config B)

Code:

<key>CFBundleURLTypes</key>
<array>
<dict>
<key>CFBundleURLSchemes</key>
<array>
<!-- e.g., "my-app-sidekick" -->
<string>$(PRODUCT_BUNDLE_IDENTIFIER)-sidekick</string>
</array>
<key>CFBundleURLName</key>
<string>com.sidekick.launch</string>
</dict>
</array>


2.  Handle the URL in `AppDelegate.swift`:


func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
// Call the SDK helper. It will check the sourceApplication
// bundle ID, and if it matches the companion app,
// it will present the Sidekick UI.
return SidekickHelper.handleUrl(url: url, sourceBundleId: options[.sourceApplication])
}


5.3. WebAssembly (Wasm)

Activation Model: Wasm activation cannot rely on a trusted companion app in the same way. It will use a combination of URL parameters and manual developer console triggers.

Companion App: The mobile companion app (Android/iOS) does not interact with the Wasm target. A future "Browser Extension Companion" could be considered but is outside this scope.

sidekick-debug (Wasm Implementation):

The GestureListener will attach DOM event listeners (e.g., a 5-tap sequence on document.body or a Konami code).

The ActivationListener will check the URL on load for a query parameter (e.g., ?sidekick=true).

Security: Low, and appropriate for a debug build.

sidekick-production (Wasm Implementation - Config B):

Activation: No listeners are registered by default. GestureListener is disabled. ActivationListener (Wasm) does nothing.

Developer Responsibility: This is the critical part. To maintain security, the developer must manually and securely expose the launch mechanism. The SDK will not expose Sidekick to the public internet.

Example: The developer adds a file to their wasmJsRelease source set:

// In src/wasmJsRelease/kotlin/com/myapp/WasmSidekickActivator.kt
@OptIn(ExperimentalJsExport::class)
@JsExport
fun launchSidekick() {
// This function internally calls code to show the SidekickShell.
// e.g., it updates a mutableStateOf<Boolean> that controls
// the SidekickShell composable's visibility.
}


Usage: A developer needing to debug the production website can open the F12 DevTools console and type window.launchSidekick() to open the UI. This maintains the "secure and intentional" activation principle.

6. Development Plan: Phased Tasks

This plan breaks the project into distinct, testable phases.

Task 1: Project Setup & Core API (sidekick-plugin-api)

Actions:

Create the KMP project with all modules (including android, ios, jvm, wasmJs targets).

Define and document the SidekickPlugin interface in SidekickPlugin.kt.

Test: Project compiles for all targets.

Task 2: Preferences API Wrapper (sidekick-preferences-api)

Actions:

Add DataStore dependency.

Implement SidekickPref<T> sealed class.

Implement SidekickPreferences class with getFlow and setValue.

Ensure DataStore is initialized correctly on each platform (e.g., localStorage for Wasm).

Test: Unit tests for SidekickPreferences (mocking DataStore) pass on all targets.

Task 3: Core No-Op Facade (sidekick-noop)

Actions:

Create Sidekick.kt with the empty register function.

Create the empty @Composable fun SidekickShell(...).

Test: Module compiles and has no dependencies other than Compose.

Task 4: Preferences UI Plugin (sidekick-plugin-preferences)

Actions:

Implement PreferencesPlugin class.

Implement Content() Composable with LazyColumn.

Create @Preview Composables for BooleanPreferenceRow, etc.

Test: @Preview composables render correctly in Android Studio.

Task 5: Core UI Implementation (sidekick-core)

Actions:

Create PluginRegistry.kt internal object.

Create InternalSidekickShell.kt Composable.

Implement adaptive navigation (e.g., NavigationRail for wide, BottomNavigation for compact) that reads from PluginRegistry.plugins.

Test: @Preview for InternalSidekickShell (with mock plugins) renders and works.

Task 6: Debug Facade (sidekick-debug)

Actions:

Implement Sidekick.kt register(): calls PluginRegistry.plugins.addAll(), GestureListener.start(), ActivationListener.start().

Implement @Composable fun SidekickShell(...) to delegate to InternalSidekickShell.

Implement GestureListener (e.g., shake detector for mobile, DOM listener for Wasm).

Implement ActivationListener (e.g., placeholder for mobile, URL query param ?sidekick=true check for Wasm).

Test: Module compiles. Wasm URL param check successfully triggers Sidekick.

Task 7: Production Facade (sidekick-production)

Actions:

Implement Sidekick.kt register(): calls PluginRegistry.plugins.addAll(), ActivationListener.start() (no gesture listener).

Implement @Composable fun SidekickShell(...) to delegate to InternalSidekickShell.

Implement Wasm ActivationListener as a no-op (as activation is manual).

Test: Module compiles.

Task 8: Demo App Integration (demo-app)

Actions:

Create demo-app KMP module (Android, iOS, Wasm).

Create MyAppLogic in commonMain (with platform-specific createDataStore for Wasm).

Create MyAppSidekick in debug and release source sets.

Test Config A: Set Gradle for releaseImplementation("sidekick-noop"). Build a release app for all targets.

Test Config B: Set Gradle for releaseImplementation("sidekick-production"). Build a release app for all targets.

Test Debug: Build a debug app for all targets.

Test:

debug build: In-app gesture/URL param opens Sidekick.

release (Config A) build: App is small, no Sidekick.

release (Config B) build: App is larger, in-app gesture/URL param does nothing.

Wasm (Config B): Test that calling window.launchSidekick() (after implementing the JsExport in the demo app) does launch Sidekick.

Task 9: Companion App & Listeners (Full Implementation)

Actions:

SDK: Implement the real ActivationListener logic (the SidekickActivity and URL scheme handler) in sidekick-core for Android and iOS.

SDK: Update sidekick-debug and sidekick-production to call the real listeners.

App: Create the sidekick-companion-app KMP project (Android and iOS).

App: Implement the Android (signature check, intent) and iOS (URL scheme) launcher logic.

Demo App: Add the required AndroidManifest.xml and Info.plist entries for both debug and release (for Config B).

Test:

Companion app launches Sidekick in debug demo app.

Companion app fails to launch in release (Config A) demo app.

Companion app succeeds in launching Sidekick in release (Config B) demo app.