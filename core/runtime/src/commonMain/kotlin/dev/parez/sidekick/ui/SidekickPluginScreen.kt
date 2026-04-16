package dev.parez.sidekick.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.parez.sidekick.SidekickState
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
internal fun SidekickPluginScreen(plugin: SidekickPlugin, state: SidekickState) {
    Box(modifier = Modifier.fillMaxSize()) {
        plugin.Content { state.backToList() }
    }
}
