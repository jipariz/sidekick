package dev.parez.sidekick.network.ui

// ── URL helpers ──────────────────────────────────────────────────────────────

internal fun urlHost(url: String): String {
    val stripped = url.removePrefix("https://").removePrefix("http://")
    return stripped.substringBefore('/').substringBefore('?').substringBefore('#')
}

internal fun urlPath(url: String): String {
    val stripped = url.removePrefix("https://").removePrefix("http://")
    val slash = stripped.indexOf('/')
    return if (slash >= 0) stripped.substring(slash) else ""
}

// ── HTTP status text ──────────────────────────────────────────────────────────

internal fun statusText(code: Int): String = when (code) {
    200 -> "OK"
    201 -> "Created"
    202 -> "Accepted"
    204 -> "No Content"
    301 -> "Moved"
    302 -> "Found"
    304 -> "Not Modified"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    405 -> "Method Not Allowed"
    408 -> "Timeout"
    409 -> "Conflict"
    422 -> "Unprocessable"
    429 -> "Too Many Requests"
    500 -> "Server Error"
    502 -> "Bad Gateway"
    503 -> "Unavailable"
    504 -> "Gateway Timeout"
    else -> ""
}

// ── Body size ────────────────────────────────────────────────────────────────

internal fun String.bodySizeLabel(): String {
    val bytes = encodeToByteArray().size
    return when {
        bytes < 1_024 -> "${bytes}B"
        bytes < 1_048_576 -> "${bytes / 1_024}KB"
        else -> "${bytes / 1_048_576}MB"
    }
}

// ── JSON pretty-printer ──────────────────────────────────────────────────────

internal fun String.prettyPrintJson(): String {
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
                    sb.append(c); sb.append('\n')
                    indent++; sb.append("  ".repeat(indent))
                }
                '}', ']' -> {
                    sb.append('\n')
                    indent--; sb.append("  ".repeat(indent))
                    sb.append(c)
                }
                ',' -> {
                    sb.append(c); sb.append('\n')
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
