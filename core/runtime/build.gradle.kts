plugins {
    id("sidekick.kmp.library")
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.navigation3.runtime)
            implementation(libs.kotlinx.navigation3.ui)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.core"
}
