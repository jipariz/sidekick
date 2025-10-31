plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(compose.materialIconsExtended)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.prefs)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.preferences"
}
