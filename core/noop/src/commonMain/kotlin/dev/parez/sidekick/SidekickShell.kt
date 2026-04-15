package dev.parez.sidekick

import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickColors
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = null,
    sidekickColors: SidekickColors? = null,
    content: @Composable () -> Unit,
) = content()
