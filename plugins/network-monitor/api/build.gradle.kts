plugins {
    id("sidekick.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.asyncExtensions)
            implementation(libs.sqldelight.primitiveAdapters)
            implementation(compose.materialIconsExtended)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.driver.sqlite)
        }
        // web-worker-driver only publishes a js variant, not wasmJs.
        // wasmJsMain returns null from createNetworkMonitorDriver() and falls back to in-memory.
        jsMain.dependencies {
            implementation(libs.sqldelight.driver.web)
        }
    }
}

sqldelight {
    databases {
        create("NetworkMonitorDatabase") {
            packageName = "dev.parez.sidekick.network.db"
            generateAsync = true
        }
    }
}

android {
    namespace = "dev.parez.sidekick.network"
}
