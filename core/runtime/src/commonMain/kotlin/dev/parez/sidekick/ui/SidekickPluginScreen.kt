package dev.parez.sidekick.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import dev.parez.sidekick.SidekickState
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickLifecycleAware
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
internal fun SidekickPluginScreen(plugin: SidekickPlugin, state: SidekickState) {
    DisposableEffect(plugin) {
        (plugin as? SidekickLifecycleAware)?.onAttach()
        onDispose { (plugin as? SidekickLifecycleAware)?.onDetach() }
    }
    CompositionLocalProvider(LocalSidekickBackNavigator provides { state.backToList() }) {
        Box(modifier = Modifier.fillMaxSize()) {
            plugin.Content()
        }
    }
}
