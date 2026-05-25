plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.vanniktechMavenPublish) apply false
    // ktfmt — Kotlin formatter applied to every subproject below.
    alias(libs.plugins.ktfmt) apply false
    // Registers `updateModuleVersions` + `checkModuleVersions` tasks.
    id("sidekick.version.update")
}

val artifactIdMap = mapOf(
    ":core:plugin-api" to "plugin-api",
    ":core:shell" to "shell",
    ":core:noop" to "noop",
    ":plugins:preferences:api" to "preferences",
    ":plugins:preferences:ksp" to "preferences-ksp",
    ":plugins:network-monitor:api" to "network-monitor",
    ":plugins:network-monitor:ui" to "network-monitor-ui",
    ":plugins:network-monitor:ktor" to "network-monitor-ktor",
    ":plugins:network-monitor:noop" to "network-monitor-noop",
    ":plugins:log-monitor:api" to "log-monitor",
    ":plugins:log-monitor:ui" to "log-monitor-ui",
    ":plugins:log-monitor:kermit" to "log-monitor-kermit",
    ":plugins:log-monitor:noop" to "log-monitor-noop",
    ":plugins:custom-screen:api" to "custom-screen",
)

subprojects {
    if (artifactIdMap.containsKey(path)) {
        ext.set("sidekick.artifactId", artifactIdMap[path])
    }

    // Apply ktfmt to every subproject — it auto-detects Kotlin source sets
    // (commonMain/androidMain/iosMain/etc. for KMP modules, plus main/test
    // for the BOM and KSP modules). Run `./gradlew ktfmtFormat` to format,
    // `./gradlew ktfmtCheck` to verify (the latter is CI-friendly).
    apply(plugin = rootProject.libs.plugins.ktfmt.get().pluginId)
    extensions.configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
        // Kotlinlang style: 4-space indent, max line 100. Matches the
        // existing codebase conventions; switch to googleStyle() if we
        // ever migrate to 2-space.
        kotlinLangStyle()
    }
}