package dev.parez.sidekick.preferences

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Properties

open class SidekickPreferencesExtension {
    /** Set to false when you supply the kspCommonMainMetadata dependency yourself (e.g. local composite builds). */
    var addProcessor: Boolean = true
}

class SidekickPreferencesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val version = readVersion()
        val extension = target.extensions.create("sidekickPreferences", SidekickPreferencesExtension::class.java)

        // KSP must be applied before KMP targets are finalized so it can register
        // per-target configurations (including kspCommonMainMetadata).
        target.pluginManager.apply("com.google.devtools.ksp")

        // withId fires immediately if KMP is already applied, or deferred until it is —
        // so plugin order in the consumer's plugins block doesn't matter.
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kmp = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
            val kspOutDir = target.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
            // Stable directory: not declared as a KSP task output, so Gradle never cleans it
            // between incremental builds even when kspCommonMainKotlinMetadata re-runs and KSP
            // skips generation (incremental mode, no annotation changes). This prevents the
            // "Unresolved reference" failure that occurs when the KSP output dir is wiped but
            // KSP generates nothing because sources haven't changed.
            val stableDir = target.layout.buildDirectory.dir("generated/sidekick-preferences/commonMain/kotlin")

            val syncKspOutputs = target.tasks.register("syncSidekickPreferencesKsp", org.gradle.api.tasks.Sync::class.java) { sync ->
                sync.from(kspOutDir)
                sync.into(stableDir)
                sync.dependsOn("kspCommonMainKotlinMetadata")
            }

            // Explicit parameter required — lambda-with-receiver for Action<T> is only
            // available in .kts files via the kotlin-dsl plugin.
            kmp.sourceSets.named("commonMain").configure { commonMain ->
                commonMain.kotlin.srcDir(stableDir)
            }

            // afterEvaluate lets the consumer configure the extension before we read it.
            target.afterEvaluate {
                if (extension.addProcessor) {
                    target.dependencies.add("kspCommonMainMetadata", "dev.parez.sidekick:preferences-ksp:$version")
                }
            }

            // tasks.configureEach + if is required for Gradle configuration cache compatibility;
            // tasks.matching { }.configureEach breaks it.
            target.tasks.configureEach { task ->
                if (task.name != "kspCommonMainKotlinMetadata" &&
                    task.name != "syncSidekickPreferencesKsp" &&
                    ((task.name.startsWith("compile") && task.name.contains("Kotlin")) || task.name.startsWith("ksp"))
                ) {
                    task.dependsOn(syncKspOutputs)
                }
            }
            target.tasks.configureEach { task ->
                if (task.name == "kspCommonMainKotlinMetadata") {
                    task.outputs.cacheIf { false }
                }
            }
        }
    }

    private fun readVersion(): String {
        val props = Properties()
        SidekickPreferencesPlugin::class.java
            .getResourceAsStream("/sidekick-preferences.properties")
            ?.use { props.load(it) }
        return props.getProperty("version", "unspecified")
    }
}
