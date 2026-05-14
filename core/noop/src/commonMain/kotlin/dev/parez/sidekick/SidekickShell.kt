package dev.parez.sidekick

import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickAppInfo
import dev.parez.sidekick.plugin.SidekickPlugin

/**
 * Release-variant SidekickShell — renders the host's content and nothing else.
 * No FAB, no overlay, no Sidekick state.
 */
@Composable
fun SidekickShell(
    plugins: List<SidekickPlugin>,
    appInfo: SidekickAppInfo? = null,
    useSidekickTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    content()
}
