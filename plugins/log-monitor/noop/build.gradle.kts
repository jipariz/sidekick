plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            api(libs.androidx.paging.common)
            api(libs.koin.core)
            api(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            // kermit is compileOnly — mirrors the real :kermit module so consumers
            // bring their own Kermit version.
            compileOnly(libs.kermit)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.log.noop"
}
