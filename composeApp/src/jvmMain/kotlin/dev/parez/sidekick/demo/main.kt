package dev.parez.sidekick.demo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sidekick Demo",
        state = rememberWindowState(width = 480.dp, height = 720.dp),
    ) {
        DemoApp()
    }
}
