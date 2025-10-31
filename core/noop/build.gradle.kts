plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.noop"
}
