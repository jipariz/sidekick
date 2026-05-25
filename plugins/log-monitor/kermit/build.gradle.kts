plugins { id("sidekick.kmp.library") }

kotlin {
    androidLibrary { namespace = "dev.parez.sidekick.log.kermit" }
    sourceSets {
        commonMain.dependencies {
            // api: the kermit module ships a factory that returns a
            // LogMonitorPlugin pre-wired with the Kermit bridge, so consumers
            // depending on :kermit get the plugin transitively without a
            // separate `:log-monitor:plugin` declaration.
            api(projects.plugins.logMonitor.api)
            api(projects.plugins.logMonitor.ui)
            // kermit is compileOnly — consumers bring their own Kermit version
            compileOnly(libs.kermit)
        }
    }
}
