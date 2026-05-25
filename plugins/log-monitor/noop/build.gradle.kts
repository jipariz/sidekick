plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.log.noop" }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            api(libs.androidx.paging.common)
            api(libs.koin.core)
            api(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            // kermit is compileOnly — mirrors the real :kermit module so consumers
            // bring their own Kermit version.
            compileOnly(libs.kermit)
        }
    }
}
