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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        jsMain.dependencies {
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
