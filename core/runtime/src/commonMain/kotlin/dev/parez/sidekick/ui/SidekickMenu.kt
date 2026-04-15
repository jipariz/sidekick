package dev.parez.sidekick.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.parez.sidekick.PluginListKey
import dev.parez.sidekick.PluginScreenKey
import dev.parez.sidekick.SidekickState

@Composable
internal fun SidekickMenu(state: SidekickState) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            val activePlugin = state.activePlugin
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (activePlugin != null) {
                    IconButton(onClick = { state.backToList() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = activePlugin?.title ?: "Sidekick",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { state.close() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Content — Navigation 3
            NavDisplay(
                backStack = state.backStack,
                entryProvider = entryProvider {
                    entry<PluginListKey> {
                        SidekickPluginList(state)
                    }
                    entry<PluginScreenKey> { key ->
                        val plugin = state.plugins.firstOrNull { it.id == key.pluginId }
                        if (plugin != null) {
                            SidekickPluginScreen(plugin)
                        }
                    }
                },
            )
        }
    }
}
