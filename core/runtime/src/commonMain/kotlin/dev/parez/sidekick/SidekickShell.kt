package dev.parez.sidekick

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.ui.SidekickMenu

@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    state: SidekickState = rememberSidekickState(plugins),
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        SmallFloatingActionButton(
            onClick = { state.open() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.BugReport, contentDescription = "Open Sidekick")
        }
        if (state.isOpen) {
            SidekickMenu(state)
        }
    }
}
