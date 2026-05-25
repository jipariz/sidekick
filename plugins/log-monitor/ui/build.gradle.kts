plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.log.ui" }
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.logMonitor.api)
            api(projects.core.pluginApi)
            implementation(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.compose.adaptive)
            implementation(libs.compose.adaptive.layout)
            implementation(libs.compose.adaptive.navigation)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.paging.compose)
        }
        androidMain.dependencies { implementation(libs.koin.android) }
    }
}
