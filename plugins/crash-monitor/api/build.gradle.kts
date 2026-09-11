plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.crash" }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.kotlinx.coroutinesCore)
            api(libs.koin.core)
        }
        androidMain.dependencies { implementation(libs.koin.android) }
    }
}
