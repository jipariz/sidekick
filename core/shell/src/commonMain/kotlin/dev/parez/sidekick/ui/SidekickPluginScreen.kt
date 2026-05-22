package dev.parez.sidekick.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import dev.parez.sidekick.SidekickState
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickLifecycleAware
import dev.parez.sidekick.plugin.SidekickPlugin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SidekickPluginScreen(plugin: SidekickPlugin, state: SidekickState) {
    DisposableEffect(plugin) {
        (plugin as? SidekickLifecycleAware)?.onAttach()
        onDispose { (plugin as? SidekickLifecycleAware)?.onDetach() }
    }
    val backNavigator = remember(state) { { state.backToList() } }
    // System / gesture back returns to the plugin list on Android; no-op on
    // other targets, which is fine — they have their own back affordances.
    // TODO: migrate to androidx.navigationevent.NavigationEventHandler once we
    // wire LocalNavigationEventDispatcherOwner at the Sidekick root composable.
    @Suppress("DEPRECATION")
    BackHandler(onBack = backNavigator)
    CompositionLocalProvider(LocalSidekickBackNavigator provides backNavigator) {
        Box(modifier = Modifier.fillMaxSize()) {
            plugin.Content()
        }
    }
}
