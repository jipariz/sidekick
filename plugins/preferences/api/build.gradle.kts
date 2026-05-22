plugins {
    id("sidekick.kmp.library")
}
kotlin {
    androidLibrary {
        namespace = "dev.parez.sidekick.preferences"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.compose.material.iconsExtended)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.prefs)
        }
    }
}
