import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.demo.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        jsMain.dependencies {
            implementation(npm("sql.js", "1.10.3"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            implementation(
                npm("sqlite-wasm-worker", layout.projectDirectory.dir("sqlite-worker").asFile)
            )
        }
        wasmJsMain.dependencies {
            implementation(
                npm("sqlite-wasm-worker", layout.projectDirectory.dir("sqlite-worker").asFile)
            )
        }
    }
}
