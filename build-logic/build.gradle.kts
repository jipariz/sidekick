plugins {
    `kotlin-dsl`
}

group = "dev.parez.sidekick.buildlogic"

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
    compileOnly(libs.plugins.androidKotlinMultiplatformLibrary.toDep())
    compileOnly(libs.plugins.composeMultiplatform.toDep())
    compileOnly(libs.plugins.composeCompiler.toDep())
    compileOnly(libs.plugins.vanniktechMavenPublish.toDep())
    // AGP DSL types (KotlinMultiplatformAndroidLibraryExtension, etc.) used by
    // SidekickKmpLibraryPlugin. The plugin-marker above resolves to this jar
    // at runtime, but the convention plugin needs the typed API at compile time.
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
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
