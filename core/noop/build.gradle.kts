plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            // Mirror runtime's api(compose.materialIconsExtended) so commonMain
            // call sites that reference Icons.Default.* still compile when the
            // Android release variant resolves to noop.
            api(compose.materialIconsExtended)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.noop"
}
