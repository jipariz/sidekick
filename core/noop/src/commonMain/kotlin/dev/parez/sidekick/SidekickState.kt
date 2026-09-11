package dev.parez.sidekick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.parez.sidekick.plugin.SidekickPlugin

public class SidekickState(public val plugins: List<SidekickPlugin>) {
    public val orderedPlugins: List<SidekickPlugin>
        get() = plugins

    public val activePlugin: SidekickPlugin?
        get() = null

    public fun reset() {}
}

@Composable
public fun rememberSidekickState(plugins: List<SidekickPlugin>): SidekickState =
    remember(plugins) { SidekickState(plugins) }
