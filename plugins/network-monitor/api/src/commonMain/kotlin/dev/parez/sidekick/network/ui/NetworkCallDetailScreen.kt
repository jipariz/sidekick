package dev.parez.sidekick.network.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.parez.sidekick.network.NetworkCall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NetworkCallDetailScreen(call: NetworkCall, onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = call.url,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Request") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Response") })
        }

        when (selectedTab) {
            0 -> RequestTab(call)
            1 -> ResponseTab(call)
        }
    }
}

@Composable
private fun RequestTab(call: NetworkCall) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionLabel("URL")
        MonoText(call.url)

        SectionLabel("Method")
        MonoText(call.method)

        if (call.requestHeaders.isNotEmpty()) {
            SectionLabel("Headers")
            HeadersTable(call.requestHeaders)
        }

        call.requestBody?.let {
            SectionLabel("Body")
            MonoText(it.prettyPrintJson())
        }
    }
}

@Composable
private fun ResponseTab(call: NetworkCall) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        call.responseCode?.let {
            SectionLabel("Status")
            MonoText("$it")
        }

        call.durationMs?.let {
            SectionLabel("Duration")
            MonoText("${it}ms")
        }

        if (call.responseHeaders.isNotEmpty()) {
            SectionLabel("Headers")
            HeadersTable(call.responseHeaders)
        }

        call.responseBody?.let {
            SectionLabel("Body")
            MonoText(it.prettyPrintJson())
        }

        call.error?.let {
            SectionLabel("Error")
            MonoText(it)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    HorizontalDivider()
}

@Composable
private fun MonoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun HeadersTable(headers: Map<String, String>) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        headers.forEach { (key, value) ->
            Text(
                text = "$key: $value",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            )
        }
    }
}

// Very simple JSON pretty-printer: indents braces and brackets.
private fun String.prettyPrintJson(): String {
    val s = trim()
    if (!s.startsWith('{') && !s.startsWith('[')) return this
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    for (i in s.indices) {
        val c = s[i]
        val prev = if (i > 0) s[i - 1] else '\u0000'
        if (c == '"' && prev != '\\') inString = !inString
        if (!inString) {
            when (c) {
                '{', '[' -> {
                    sb.append(c)
                    sb.append('\n')
                    indent++
                    sb.append("  ".repeat(indent))
                }
                '}', ']' -> {
                    sb.append('\n')
                    indent--
                    sb.append("  ".repeat(indent))
                    sb.append(c)
                }
                ',' -> {
                    sb.append(c)
                    sb.append('\n')
                    sb.append("  ".repeat(indent))
                }
                ':' -> sb.append(": ")
                else -> sb.append(c)
            }
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}
