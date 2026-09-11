package dev.parez.sidekick.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickPlugin
import dev.parez.sidekick.preferences.ui.PreferencesContent
import kotlinx.coroutines.flow.StateFlow

public abstract class PreferencesPlugin(
    pluginTitle: String,
    public val definitions: List<PreferenceDefinition<*>>,
    public val valueFlows: Map<String, StateFlow<Any>>,
    public val onSet: suspend (key: String, value: Any) -> Unit,
) : SidekickPlugin {
    override val id: String = "sidekick.preferences.${pluginTitle.lowercase().replace(" ", "_")}"
    override val title: String = pluginTitle
    override val icon: ImageVector = Icons.Default.Settings

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        PreferencesContent(
            title = title,
            definitions = definitions,
            valueFlows = valueFlows,
            onSet = onSet,
            onBack = navigateBack,
        )
    }
}
