package dev.parez.sidekick.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.SidekickState
import dev.parez.sidekick.plugin.SidekickAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SidekickPluginList(
    state: SidekickState,
    title: String,
    appInfo: SidekickAppInfo?,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Scaffold(
        topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    },
                    navigationIcon = navigationIcon,
                    actions = actions,
                )
        },
    ) {
        LazyVerticalGrid(
            modifier = Modifier.padding(it),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            stickyHeader {
                if (appInfo != null) {
                    AppInfoStrip(appInfo)
                }
            }
            items(state.plugins, key = { it.id }) { plugin ->
                ElevatedCard(
                    onClick = { state.selectPlugin(plugin) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = plugin.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Text(
                            text = plugin.title,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
