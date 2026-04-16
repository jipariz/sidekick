package dev.parez.sidekick

import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickColors
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
fun Sidekick(
    plugins: List<SidekickPlugin>,
    onClose: () -> Unit,
    appInfo: SidekickAppInfo? = null,
    state: SidekickState = rememberSidekickState(plugins),
    sidekickColors: SidekickColors? = null,
) = Unit
