package dev.parez.sidekick

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.parez.sidekick.gesture.fiveTapDetector
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.ui.SidekickMenu

@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    state: SidekickState = rememberSidekickState(plugins),
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().fiveTapDetector { state.open() }) {
        content()
        if (state.isOpen) {
            SidekickMenu(state)
        }
    }
}
