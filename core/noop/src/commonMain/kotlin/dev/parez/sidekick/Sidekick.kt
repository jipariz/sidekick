package dev.parez.sidekick

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
fun Sidekick(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = null,
    state: SidekickState = rememberSidekickState(plugins),
    useSidekickTheme: Boolean = true,
    title: String = "Sidekick",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) = Unit
