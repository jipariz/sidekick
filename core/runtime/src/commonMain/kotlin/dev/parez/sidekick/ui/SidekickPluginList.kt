package dev.parez.sidekick.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.parez.sidekick.SidekickState

@Composable
internal fun SidekickPluginList(state: SidekickState) {
    LazyColumn {
        items(state.plugins) { plugin ->
            ListItem(
                headlineContent = { Text(plugin.title) },
                leadingContent = { Icon(plugin.icon, contentDescription = null) },
                modifier = Modifier.clickable { state.selectPlugin(plugin) },
            )
        }
    }
}
