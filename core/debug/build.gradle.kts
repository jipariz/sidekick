plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.runtime)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.debug"
}
