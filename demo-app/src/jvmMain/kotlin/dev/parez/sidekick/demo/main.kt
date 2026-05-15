package dev.parez.sidekick.demo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val screenshotConfig = ScreenshotConfig(target = System.getenv("SIDEKICK_SHOT"))
    val windowState = if (screenshotConfig.isActive) {
        rememberWindowState(
            position = WindowPosition(0.dp, 0.dp),
            size = DpSize(width = 1280.dp, height = 800.dp),
        )
    } else {
        rememberWindowState(width = 480.dp, height = 720.dp)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sidekick Demo",
        state = windowState,
    ) {
        DemoApp(
            screenshotConfig = screenshotConfig,
            nowMillis = { System.currentTimeMillis() },
        )
    }
}
