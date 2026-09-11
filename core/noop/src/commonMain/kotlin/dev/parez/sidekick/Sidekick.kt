package dev.parez.sidekick

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
public fun Sidekick(
    plugins: List<SidekickPlugin>,
    modifier: Modifier = Modifier,
    appInfo: SidekickAppInfo? = null,
    useSidekickTheme: Boolean = true,
    title: String = "Sidekick",
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
): Unit = Unit
