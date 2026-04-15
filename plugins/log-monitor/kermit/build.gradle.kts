plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.plugins.logMonitor.api)
            // kermit is compileOnly — consumers bring their own Kermit version
            compileOnly(libs.kermit)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.logs.kermit"
}
