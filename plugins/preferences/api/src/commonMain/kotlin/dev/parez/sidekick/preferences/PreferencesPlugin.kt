package dev.parez.sidekick.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.preferences.ui.PreferencesContent
import kotlinx.coroutines.flow.StateFlow

abstract class PreferencesPlugin(
    private val pluginTitle: String,
    val definitions: List<PreferenceDefinition<*>>,
    val valueFlows: Map<String, StateFlow<Any>>,
    val onSet: suspend (key: String, value: Any) -> Unit,
) : SidekickPlugin {
    override val id: String = "sidekick.preferences.${pluginTitle.lowercase().replace(" ", "_")}"
    override val title: String = pluginTitle
    override val icon = Icons.Default.Settings

    @Composable
    override fun Content() {
        PreferencesContent(
            definitions = definitions,
            valueFlows = valueFlows,
            onSet = onSet,
        )
    }
}
