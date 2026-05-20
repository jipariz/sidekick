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
}