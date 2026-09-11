package dev.parez.sidekick.demo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.parez.sidekick.crash.CrashMonitor

fun main() {
    CrashMonitor.install(appPackagePrefix = "dev.parez.sidekick.demo")
    launchApp()
}

private fun launchApp() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sidekick Demo",
        state = rememberWindowState(width = 480.dp, height = 720.dp),
    ) {
        DemoApp()
    }
}
