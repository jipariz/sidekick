plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.crash.noop" }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            api(libs.koin.core)
            implementation(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
        }
    }
}
