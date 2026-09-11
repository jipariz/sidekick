plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.database.noop" }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            api(libs.koin.core)
            implementation(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            // Mirrors :room — consumers bring their own Room version, and the noop
            // variant never touches it.
            compileOnly(libs.room3.runtime)
        }
    }
}
