package dev.parez.sidekick.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.SidekickPlugin

/**
 * A [SidekickPlugin] that renders any Composable as a first-class debug screen
 * in the Sidekick overlay.
 *
 * Each instance appears as its own card in the plugin grid. Create as many
 * instances as needed and pass them all to [dev.parez.sidekick.SidekickShell].
 *
 * Any DI framework (Koin, Hilt, custom [androidx.compose.runtime.CompositionLocal]s, …)
 * works inside [content] because it executes inside the host app's composition tree —
 * Sidekick does not override DI-related CompositionLocals.
 *
 * @param id      Unique identifier for this screen (kebab-case recommended).
 * @param title   Label shown in the plugin grid card and screen header.
 * @param icon    Icon shown in the plugin grid card.
 * @param content Composable rendered when the user opens this screen.
 */
class CustomScreenPlugin(
    override val id: String,
    override val title: String,
    override val icon: ImageVector,
    private val content: @Composable () -> Unit,
) : SidekickPlugin {

    @Composable
    override fun Content() = content()
}
