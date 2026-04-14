import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.networkMonitor.api)
            // Optional log-monitor integration: auto-emits HTTP logs with networkCallId metadata
            implementation(projects.plugins.logMonitor.api)
            // ktor-client-core is compileOnly — consumers bring their own Ktor version
            compileOnly("io.ktor:ktor-client-core:3.1.3")
        }
    }
}

android {
    namespace = "dev.parez.sidekick.network.ktor"
}
