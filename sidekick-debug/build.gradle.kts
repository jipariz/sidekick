plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.sidekickCore)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.debug"
}
