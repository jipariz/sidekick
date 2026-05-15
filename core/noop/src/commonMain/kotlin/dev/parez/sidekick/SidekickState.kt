package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.parez.sidekick.plugin.SidekickPlugin

class SidekickState(val plugins: List<SidekickPlugin>) {
    val activePlugin: SidekickPlugin? get() = null
    fun reset() {}
}

@Composable
fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }
