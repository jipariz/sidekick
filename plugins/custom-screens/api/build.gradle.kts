plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(compose.materialIconsExtended)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.screens"
}
