package dev.parez.sidekick.demo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.parez.sidekick.crash.CrashMonitor
import dev.parez.sidekick.plugin.ApplicationContextHolder

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.initialize(this)
        // Installed here, not from the plugin's constructor: plugins are built when
        // the overlay is first composed, which is far too late to catch a startup
        // crash. CrashStorage needs the context holder, hence the ordering.
        CrashMonitor.install(appPackagePrefix = "dev.parez.sidekick.demo")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoApp()
        }
    }
}
