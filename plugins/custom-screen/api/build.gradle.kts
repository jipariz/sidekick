plugins {
    id("sidekick.kmp.library")
}
kotlin {
    androidLibrary {
        namespace = "dev.parez.sidekick.screen"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.compose.material.iconsExtended)
        }
    }
}
