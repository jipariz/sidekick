plugins {
    id("sidekick.kmp.library")
}
kotlin {
    androidLibrary {
        // AGP requires a unique namespace per library, so this cannot match the
        // source package `dev.parez.sidekick` (which is shared with :core:shell so
        // the Android release variant swap works).
        namespace = "dev.parez.sidekick.shell.noop"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.pluginApi)
            // Mirror shell's api(compose.materialIconsExtended) so commonMain
            // call sites that reference Icons.Default.* still compile when the
            // Android release variant resolves to noop.
            api(compose.materialIconsExtended)
        }
    }
}
