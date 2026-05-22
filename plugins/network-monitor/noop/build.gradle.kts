plugins {
    id("sidekick.kmp.library")
}
kotlin {
    androidLibrary {
        namespace = "dev.parez.sidekick.network.noop"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            api(libs.androidx.paging.common)
            api(libs.koin.core)
            api(libs.compose.material.iconsExtended)
            implementation(libs.kotlinx.coroutinesCore)
            // ktor-client-core is compileOnly here — mirrors the :ktor module so
            // the noop NetworkMonitorKtor ClientPlugin compiles without forcing
            // consumers to expose a Ktor version transitively.
            compileOnly(libs.ktor.client.core)
        }
    }
}
