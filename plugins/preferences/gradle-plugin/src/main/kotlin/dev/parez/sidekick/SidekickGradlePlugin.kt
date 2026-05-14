package dev.parez.sidekick

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Properties

/**
 * Umbrella Gradle plugin for Sidekick. One-stop consumer setup:
 *
 * ```kotlin
 * plugins {
 *     id("dev.parez.sidekick") version "<version>"
 * }
 *
 * sidekick {
 *     preferences()
 *     networkMonitor()
 *     logMonitor()
 *     customScreens()
 * }
 * ```
 *
 * What this gives you, automatically:
 * - `dev.parez.sidekick.preferences` is applied (which brings KSP, wires
 *   commonMain/jsMain/wasmJsMain srcDirs, and registers the preferences
 *   processor).
 * - `dev.parez.sidekick:runtime` is added to `commonMain`, so the `Sidekick()`
 *   and `SidekickShell()` composables resolve everywhere.
 * - For Android consumers, `runtime` is swapped with `noop` in release
 *   variants via `debugImplementation` / `releaseImplementation`.
 * - The plugin modules you opted into via the `sidekick { ... }` block are
 *   added to `commonMain` with the right Maven coordinates.
 *
 * The plugin does NOT add per-platform Ktor engines, Coil, Kermit, or any
 * other non-Sidekick dependency — that's still on the consumer.
 */
class SidekickGradlePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val version = readVersion()
        val sidekick = target.extensions.create("sidekick", SidekickExtension::class.java)

        // Always apply the preferences plugin — it brings KSP transitively and
        // wires KSP source-set directories. If the consumer never writes a
        // @SidekickPreferences class, KSP runs but generates nothing — no
        // material overhead.
        target.pluginManager.apply("dev.parez.sidekick.preferences")

        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            target.afterEvaluate {
                wireDependencies(target, sidekick, version)
            }
        }
    }

    private fun wireDependencies(target: Project, sidekick: SidekickExtension, version: String) {
        val kmp = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

        kmp.sourceSets.named("commonMain").configure { commonMain ->
            commonMain.dependencies {
                // compileOnly: provides the Sidekick(), SidekickShell(), and
                // SidekickState type stubs for commonMain compilation, but
                // stays out of every variant's runtime classpath. Per-platform
                // source sets add the runtime jar via implementation below;
                // Android picks runtime (debug) or noop (release) via the
                // variant swap. Without `compileOnly` here, runtime ends up in
                // Android release's runtime classpath alongside noop and AGP
                // fails the build with `checkReleaseDuplicateClasses`.
                compileOnly("dev.parez.sidekick:runtime:$version")

                if (sidekick.preferencesEnabled) {
                    implementation("dev.parez.sidekick:preferences:$version")
                }
                if (sidekick.networkMonitorEnabled) {
                    implementation("dev.parez.sidekick:network-monitor-plugin:$version")
                    implementation("dev.parez.sidekick:network-monitor-ktor:$version")
                }
                if (sidekick.logMonitorEnabled) {
                    // The :kermit module re-exports :plugin via api, so the
                    // bare LogMonitorPlugin is reachable too.
                    implementation("dev.parez.sidekick:log-monitor-kermit:$version")
                }
                if (sidekick.customScreensEnabled) {
                    implementation("dev.parez.sidekick:custom-screens:$version")
                }
            }
        }

        // Non-Android targets: real runtime in each target's runtime classpath.
        // Adding to the intermediate `iosMain` source set covers all 3 iOS
        // leaf targets (arm64, x64, simulatorArm64) via the default hierarchy.
        // findByName silently skips targets the consumer hasn't declared.
        listOf("jvmMain", "iosMain", "jsMain", "wasmJsMain").forEach { sourceSetName ->
            kmp.sourceSets.findByName(sourceSetName)?.dependencies {
                implementation("dev.parez.sidekick:runtime:$version")
            }
        }

        // Android variant swap — full overlay in debug, no-op stub in release.
        // These configurations are Android-variant-scoped, so the dep only
        // appears in the Android variant's classpath, not commonMain's.
        val isAndroidApp = target.plugins.hasPlugin("com.android.application")
        val isAndroidLib = target.plugins.hasPlugin("com.android.library")
        if (isAndroidApp || isAndroidLib) {
            target.dependencies.add("debugImplementation", "dev.parez.sidekick:runtime:$version")
            target.dependencies.add("releaseImplementation", "dev.parez.sidekick:noop:$version")
        }
    }

    private fun readVersion(): String {
        // Shares the version file with the preferences plugin (same JAR).
        val props = Properties()
        SidekickGradlePlugin::class.java
            .getResourceAsStream("/sidekick-preferences.properties")
            ?.use { props.load(it) }
        return props.getProperty("version", "unspecified")
    }
}

/**
 * DSL for selecting which Sidekick plugin modules to pull into `commonMain`.
 * Each method-call style entry is opt-in — call only what you need.
 *
 * ```kotlin
 * sidekick {
 *     networkMonitor()
 *     logMonitor()
 * }
 * ```
 */
open class SidekickExtension {
    internal var preferencesEnabled: Boolean = false
    internal var networkMonitorEnabled: Boolean = false
    internal var logMonitorEnabled: Boolean = false
    internal var customScreensEnabled: Boolean = false

    /** Adds `dev.parez.sidekick:preferences` to `commonMain`. The preferences
     *  Gradle plugin (and its KSP wiring) is applied automatically regardless;
     *  this method enables the runtime UI module. */
    fun preferences() { preferencesEnabled = true }

    /** Adds `dev.parez.sidekick:network-monitor-plugin` and `network-monitor-ktor` to `commonMain`. */
    fun networkMonitor() { networkMonitorEnabled = true }

    /** Adds `dev.parez.sidekick:log-monitor-kermit` (which re-exports `log-monitor-plugin`) to `commonMain`. */
    fun logMonitor() { logMonitorEnabled = true }

    /** Adds `dev.parez.sidekick:custom-screens` to `commonMain`. */
    fun customScreens() { customScreensEnabled = true }
}
