package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.parez.sidekick.plugin.SidekickPlugin

class SidekickState(val plugins: List<SidekickPlugin>) {
    var isOpen: Boolean by mutableStateOf(false)
        internal set
    var activePlugin: SidekickPlugin? by mutableStateOf(null)
        internal set

    internal fun open() { isOpen = true }
    internal fun close() { isOpen = false; activePlugin = null }
    internal fun selectPlugin(p: SidekickPlugin) { activePlugin = p }
    internal fun backToList() { activePlugin = null }
}

@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }
