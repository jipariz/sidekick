plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.networkMonitor.api)
            api(projects.core.pluginApi)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.network.plugin"
}
