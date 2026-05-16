plugins {
    `kotlin-dsl`
}

group = "dev.parez.sidekick.buildlogic"

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
    compileOnly(libs.plugins.androidLibrary.toDep())
    compileOnly(libs.plugins.composeMultiplatform.toDep())
    compileOnly(libs.plugins.composeCompiler.toDep())
    compileOnly(libs.plugins.vanniktechMavenPublish.toDep())
}

gradlePlugin {
    plugins {
        register("sidekickKmpLibrary") {
            id = "sidekick.kmp.library"
            implementationClass = "SidekickKmpLibraryPlugin"
        }
        register("sidekickVersionRead") {
            id = "sidekick.version.read"
            implementationClass = "SidekickVersionReadConventionPlugin"
        }
        register("sidekickVersionUpdate") {
            id = "sidekick.version.update"
            implementationClass = "SidekickVersionUpdateConventionPlugin"
        }
    }
}
