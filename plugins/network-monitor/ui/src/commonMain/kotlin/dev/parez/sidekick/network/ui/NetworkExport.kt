package dev.parez.sidekick.network.ui

import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall

/**
 * Renders [call] as a `curl` invocation that reproduces it.
 *
 * Header values are whatever the monitor captured, so anything redacted at capture time stays
 * redacted here — the command will need the real credential filled in before it runs. That is the
 * intended trade: an export that leaks a bearer token is worse than one you have to edit.
 */
internal fun NetworkCall.toCurl(): String = buildString {
    // url and method go through the same escaping as the headers below: a captured
    // URL can legitimately contain an apostrophe, which would otherwise terminate the
    // shell-quoted argument and let the rest of the URL run as shell syntax on
    // whatever machine the exported command is pasted into.
    append("curl -X ")
        .append(method.escapeSingleQuotes())
        .append(" '")
        .append(url.escapeSingleQuotes())
        .append('\'')
    requestHeaders.forEach { (name, value) ->
        append(" \\\n  -H '")
            .append(name.escapeSingleQuotes())
            .append(": ")
            .append(value.escapeSingleQuotes())
            .append('\'')
    }
    requestBody?.let { append(" \\\n  --data-raw '").append(it.escapeSingleQuotes()).append('\'') }
}

/** Full human-readable dump of a single call: status line, both header sets, both bodies. */
internal fun NetworkCall.toShareText(): String = buildString {
    appendLine("$method $url")
    appendLine("Status: ${responseCode?.toString() ?: status.label()}")
    durationMs?.let { appendLine("Duration: ${it}ms") }
    error?.let { appendLine("Error: $it") }
    if (requestHeaders.isNotEmpty()) {
        appendLine()
        appendLine("── Request headers ──")
        requestHeaders.forEach { (name, value) -> appendLine("$name: $value") }
    }
    appendBody("Request body", requestBody, bodiesEvicted)
    if (responseHeaders.isNotEmpty()) {
        appendLine()
        appendLine("── Response headers ──")
        responseHeaders.forEach { (name, value) -> appendLine("$name: $value") }
    }
    appendBody("Response body", responseBody, bodiesEvicted)
    appendLine()
    appendLine("── curl ──")
    appendLine(toCurl())
}

/** One line per call, newest first — the list-screen export. */
internal fun List<NetworkCall>.toShareText(): String = buildString {
    val count = this@toShareText.size
    appendLine("Sidekick network export — $count ${if (count == 1) "call" else "calls"}")
    appendLine()
    this@toShareText.forEach { call ->
        val code = call.responseCode?.toString() ?: call.status.label()
        val duration = call.durationMs?.let { "${it}ms" } ?: "—"
        appendLine("${call.method.padEnd(6)} $code  $duration  ${call.url}")
    }
}

private fun StringBuilder.appendBody(label: String, body: String?, evicted: Boolean) {
    when {
        body != null -> {
            appendLine()
            appendLine("── $label ──")
            appendLine(body)
        }
        evicted -> {
            appendLine()
            appendLine("── $label ──")
            appendLine("(dropped to save memory)")
        }
    }
}

private fun CallStatus.label(): String =
    when (this) {
        CallStatus.PENDING -> "pending"
        CallStatus.COMPLETE -> "complete"
        CallStatus.ERROR -> "error"
    }

// Single quotes terminate the shell-quoted strings above, so they need the
// close-quote / escaped-quote / reopen-quote dance.
private fun String.escapeSingleQuotes(): String = replace("'", """'\''""")
