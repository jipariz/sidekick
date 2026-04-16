plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinJvm) apply false
}

val artifactIdMap = mapOf(
    ":core:plugin-api" to "plugin-api",
    ":core:runtime" to "runtime",
    ":core:noop" to "noop",
    ":plugins:preferences:api" to "preferences",
    ":plugins:preferences:ksp" to "preferences-ksp",
    ":plugins:network-monitor:api" to "network-monitor",
    ":plugins:network-monitor:plugin" to "network-monitor-plugin",
    ":plugins:network-monitor:ktor" to "network-monitor-ktor",
    ":plugins:log-monitor:api" to "log-monitor",
    ":plugins:log-monitor:plugin" to "log-monitor-plugin",
    ":plugins:log-monitor:kermit" to "log-monitor-kermit",
    ":plugins:custom-screens:api" to "custom-screens",
)

subprojects {
    if (artifactIdMap.containsKey(path)) {
        ext.set("sidekick.artifactId", artifactIdMap[path])
    }
}