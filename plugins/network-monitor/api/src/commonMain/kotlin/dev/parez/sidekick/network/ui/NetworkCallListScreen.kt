package dev.parez.sidekick.network.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall

@Composable
internal fun NetworkCallListScreen(
    calls: List<NetworkCall>,
    onSelect: (NetworkCall) -> Unit,
    onClear: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(calls, query) {
        if (query.isBlank()) calls
        else calls.filter { it.url.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search URL…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Delete, contentDescription = "Clear")
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No network calls recorded.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                items(filtered, key = { it.id }) { call ->
                    NetworkCallRow(call = call, onClick = { onSelect(call) })
                }
            }
        }
    }
}

@Composable
private fun NetworkCallRow(call: NetworkCall, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MethodBadge(call.method)

            Column(Modifier.weight(1f)) {
                Text(
                    text = call.url,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                call.durationMs?.let {
                    Text(
                        text = "${it}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatusChip(call)
        }
    }
}

@Composable
private fun MethodBadge(method: String) {
    val color = when (method.uppercase()) {
        "GET"    -> Color(0xFF1976D2)
        "POST"   -> Color(0xFF388E3C)
        "PUT"    -> Color(0xFFF57C00)
        "DELETE" -> Color(0xFFD32F2F)
        "PATCH"  -> Color(0xFF7B1FA2)
        else     -> Color(0xFF546E7A)
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = method.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun StatusChip(call: NetworkCall) {
    val (text, color) = when (call.status) {
        CallStatus.PENDING  -> "…"       to Color(0xFF9E9E9E)
        CallStatus.ERROR    -> "ERR"     to Color(0xFFD32F2F)
        CallStatus.COMPLETE -> {
            val code = call.responseCode ?: 0
            val c = when {
                code < 300 -> Color(0xFF388E3C)
                code < 400 -> Color(0xFF1976D2)
                code < 500 -> Color(0xFFF57C00)
                else       -> Color(0xFFD32F2F)
            }
            "$code" to c
        }
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
        )
    }
}
