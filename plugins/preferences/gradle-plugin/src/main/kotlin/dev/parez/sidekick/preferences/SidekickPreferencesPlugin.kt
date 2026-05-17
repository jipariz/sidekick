package dev.parez.sidekick.preferences

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class SidekickPreferencesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        // KSP must be applied before KMP targets are finalized so it can register
        // per-target configurations (including kspCommonMainMetadata).
        target.pluginManager.apply("com.google.devtools.ksp")

        // withId fires immediately if KMP is already applied, or deferred until it is —
        // so plugin order in the consumer's plugins block doesn't matter.
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kmp = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

            // KSP auto-registers `build/generated/ksp/metadata/commonMain/kotlin` as a
            // commonMain source dir for the `compileCommonMainKotlinMetadata` task, but
            // per-target compiles (compileDebugKotlinAndroid, compileKotlinJvm, …) read
            // commonMain srcDirs from a *different* snapshot that doesn't include KSP's
            // auto-registered dir. We bridge the gap by registering a sibling stable dir
            // that the Sync task copies KSP output into; the stable dir is on commonMain
            // for both the metadata pass and the per-target compiles. KSP's own dir is
            // also (auto-)registered, but only the metadata task sees it — no redeclaration
            // results because the metadata task scans each srcDir exactly once.
            val kspOutDir = target.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")
            val stableDir = target.layout.buildDirectory.dir("generated/sidekick-preferences/commonMain/kotlin")

            val syncKspOutputs = target.tasks.register("syncSidekickPreferencesKsp", org.gradle.api.tasks.Sync::class.java) { sync ->
                sync.from(kspOutDir)
                sync.into(stableDir)
                sync.dependsOn("kspCommonMainKotlinMetadata")
            }

            kmp.sourceSets.named("commonMain").configure { commonMain ->
                commonMain.kotlin.srcDir(stableDir)
            }

            // Per-target KSP outputs for JS / WasmJS. configureEach is lazy: it fires
            // whichever order the consumer registers the targets.
            val perTargetDirs = mapOf(
                "jsMain" to target.layout.buildDirectory.dir("generated/ksp/js/jsMain/kotlin"),
                "wasmJsMain" to target.layout.buildDirectory.dir("generated/ksp/wasmJs/wasmJsMain/kotlin"),
            )
            kmp.sourceSets.matching { it.name in perTargetDirs.keys }.configureEach { sourceSet ->
                sourceSet.kotlin.srcDir(perTargetDirs.getValue(sourceSet.name))
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

            // Always re-run the KSP metadata task. Gradle's UP-TO-DATE check is keyed on
            // input fingerprints, not on whether the output dir still exists — and the
            // dir gets cleaned in some `assembleDebug`/`allTests` paths between phases.
            // When that happens, KSP is skipped, Sync sees no source files, and per-
            // target compiles fail with "Unresolved reference" against the generated
            // accessors. Forcing re-execution costs one cheap KSP pass per build but
            // keeps the output dir authoritative. Build cache is also disabled because
            // sharing a path-sensitive output dir across machines is unsafe.
            target.tasks.configureEach { task ->
                if (task.name == "kspCommonMainKotlinMetadata") {
                    task.outputs.cacheIf { false }
                    task.outputs.upToDateWhen { false }
                }
            }
        }
    }
}
