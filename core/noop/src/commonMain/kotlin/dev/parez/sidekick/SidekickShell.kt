package dev.parez.sidekick

import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickPlugin

@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    content: @Composable () -> Unit,
) = content()
