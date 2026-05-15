package dev.parez.sidekick.network.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall

private val MethodChoices = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkCallListPane(
    lazyItems: LazyPagingItems<NetworkCall>,
    selected: NetworkCall? = null,
    query: String,
    methodFilter: Set<String>,
    filteredCount: Long,
    onSelect: (NetworkCall) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleMethod: (String) -> Unit,
    onClear: () -> Unit,
    showChevron: Boolean = true,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Network Monitor",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            // ── Search bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search URL or method…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Method filter chips ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MethodChoices.forEach { method ->
                    val isChosen = method in methodFilter
                    FilterChip(
                        selected = isChosen,
                        onClick = { onToggleMethod(method) },
                        label = {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$filteredCount request${if (filteredCount != 1L) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Refresh progress bar ──────────────────────────────────────────────
            if (lazyItems.loadState.refresh is LoadState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // ── Content ───────────────────────────────────────────────────────────
            val refresh = lazyItems.loadState.refresh
            val isEmpty = lazyItems.itemCount == 0 && refresh is LoadState.NotLoading
            when {
                isEmpty -> NetworkCallEmptyState(
                    isFiltered = query.isNotBlank() || methodFilter.isNotEmpty(),
                )

                refresh is LoadState.Error -> NetworkCallErrorState(
                    error = refresh.error,
                    onRetry = lazyItems::retry,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        count = lazyItems.itemCount,
                        key = lazyItems.itemKey { it.id },
                        contentType = lazyItems.itemContentType { "NetworkCall" },
                    ) { index ->
                        val call = lazyItems[index] ?: return@items
                        NetworkCallRow(
                            call = call,
                            isSelected = selected?.id == call.id,
                            showChevron = showChevron,
                            onClick = { onSelect(call) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 56.dp),
                        )
                    }

                    when (val appendState = lazyItems.loadState.append) {
                        is LoadState.Loading -> item { AppendLoadingRow() }
                        is LoadState.Error -> item {
                            AppendErrorRow(
                                error = appendState.error,
                                onRetry = lazyItems::retry,
                            )
                        }

                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun AppendLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AppendErrorRow(error: Throwable, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Failed to load more: ${error.message ?: error::class.simpleName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Retry", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NetworkCallErrorState(error: Throwable, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Failed to load requests",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = error.message ?: error::class.simpleName.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun NetworkCallRow(
    call: NetworkCall,
    isSelected: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
) {
    val selectionColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) selectionColor else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MethodBadge(call.method, modifier = Modifier.padding(top = 2.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val host = urlHost(call.url)
                val path = urlPath(call.url)
                if (host.isNotEmpty()) {
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (path.isNotEmpty()) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = call.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val meta = buildString {
                    call.durationMs?.let { append("${it}ms") }
                    call.responseBody?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.bodySizeLabel())
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusChip(call)
                if (showChevron) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val bg = when (method.uppercase()) {
        "GET" -> cs.primary
        "POST" -> cs.secondary
        "PUT" -> cs.tertiary
        "DELETE" -> cs.error
        "PATCH" -> cs.tertiaryContainer
        else -> cs.outline
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier,
    ) {
        Text(
            text = method.uppercase().take(6),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onPrimary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
internal fun StatusChip(call: NetworkCall) {
    val cs = MaterialTheme.colorScheme
    val (text, bg) = when (call.status) {
        CallStatus.PENDING -> "●  PENDING" to cs.outlineVariant
        CallStatus.ERROR -> "ERR" to cs.error
        CallStatus.COMPLETE -> {
            val code = call.responseCode ?: 0
            val label = "$code ${statusText(code)}".trim()
            val c = when {
                code < 300 -> cs.secondary
                code < 400 -> cs.primary
                code < 500 -> cs.tertiary
                else -> cs.error
            }
            label to c
        }
    }
    Surface(color = bg, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSecondary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun NetworkCallEmptyState(isFiltered: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = if (isFiltered) "No matching requests" else "No requests recorded",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isFiltered) "Adjust filters or search to see results" else "Make a network call to see it here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
