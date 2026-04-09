package dev.parez.sidekick.demo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.parez.sidekick.plugin.ApplicationContextHolder

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.initialize(this)
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
