plugins {
    id("sidekick.kmp.library")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.networkMonitor.api)
            api(projects.core.pluginApi)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.navigation3.runtime)
            implementation(libs.kotlinx.navigation3.ui)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.network.plugin"
}
