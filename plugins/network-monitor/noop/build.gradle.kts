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
            // ktor-client-core is compileOnly here — mirrors the :ktor module so
            // the noop NetworkMonitorKtor ClientPlugin compiles without forcing
            // consumers to expose a Ktor version transitively.
            compileOnly("io.ktor:ktor-client-core:3.1.3")
        }
    }
}

android {
    namespace = "dev.parez.sidekick.network.noop"
}
