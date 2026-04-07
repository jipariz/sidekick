package dev.parez.sidekick.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.SidekickShell
import dev.parez.sidekick.network.NetworkMonitorPlugin
import dev.parez.sidekick.network.RetentionPeriod
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.launch

@Composable
fun DemoApp() {
    val preferencesPlugin = remember { AppPreferencesPlugin() }
    val networkPlugin = remember { NetworkMonitorPlugin(retentionMs = RetentionPeriod.ONE_HOUR) }
    val darkMode by preferencesPlugin.accessor.darkMode.collectAsState()

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme() else lightColorScheme(),
    ) {
        SidekickShell(plugins = listOf(preferencesPlugin, networkPlugin)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Sidekick Demo", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tap 5 times to open Sidekick → Network tab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Text("Sample Network Calls", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Each button fires a real HTTP request. " +
                        "Open the Network monitor to inspect headers, bodies and timing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SampleCallsPanel()
            }
        }
    }
}

private data class CallResult(val label: String, val status: Int?, val error: String?)

private data class SampleCall(
    val label: String,
    val color: Color,
    val block: suspend () -> HttpResponse,
)

@Composable
private fun SampleCallsPanel() {
    val scope = rememberCoroutineScope()
    var activeCall by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<CallResult?>(null) }

    suspend fun fire(label: String, block: suspend () -> HttpResponse) {
        activeCall = label
        runCatching { block() }
            .onSuccess { lastResult = CallResult(label, it.status.value, null) }
            .onFailure { lastResult = CallResult(label, null, it.message) }
        activeCall = null
    }

    val calls = remember {
        listOf(
            SampleCall("GET /posts",      Color(0xFF1976D2)) { fetchPosts() },
            SampleCall("GET /posts/1",    Color(0xFF1976D2)) { fetchPost() },
            SampleCall("GET /users/1",    Color(0xFF1976D2)) { fetchUser() },
            SampleCall("POST /posts",     Color(0xFF388E3C)) { createPost() },
            SampleCall("PUT /posts/1",    Color(0xFFF57C00)) { updatePost() },
            SampleCall("DELETE /posts/1", Color(0xFFD32F2F)) { deletePost() },
            SampleCall("GET 404",         Color(0xFF9E9E9E)) { fetchNotFound() },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        calls.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { call ->
                    val isActive = activeCall == call.label
                    OutlinedButton(
                        onClick = { if (activeCall == null) scope.launch { fire(call.label, call.block) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = call.color),
                        enabled = activeCall == null,
                    ) {
                        if (isActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = call.color,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            call.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                // pad last row if odd number of calls
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Button(
            onClick = {
                if (activeCall == null) scope.launch {
                    for (call in calls) fire(call.label, call.block)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = activeCall == null,
        ) {
            if (activeCall != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(activeCall ?: "")
            } else {
                Text("Fire all requests")
            }
        }
    }

    lastResult?.let { result ->
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Last result",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(result.label, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                result.status?.let { code ->
                    val color = when {
                        code < 300 -> Color(0xFF388E3C)
                        code < 500 -> Color(0xFFF57C00)
                        else       -> Color(0xFFD32F2F)
                    }
                    Text("HTTP $code", style = MaterialTheme.typography.bodyMedium, color = color)
                }
                result.error?.let {
                    Text("Error: $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                }
            }
        }
    }
}
