plugins {
    id("sidekick.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // api: the kermit module ships a factory that returns a
            // LogMonitorPlugin pre-wired with the Kermit bridge, so consumers
            // depending on :kermit get the plugin transitively without a
            // separate `:log-monitor:plugin` declaration.
            api(projects.plugins.logMonitor.api)
            api(projects.plugins.logMonitor.plugin)
            // kermit is compileOnly — consumers bring their own Kermit version
            compileOnly(libs.kermit)
        }
    }
}

android {
    namespace = "dev.parez.sidekick.logs.kermit"
}
