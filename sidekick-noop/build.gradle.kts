plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.sidekickPluginApi)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.noop"
}
