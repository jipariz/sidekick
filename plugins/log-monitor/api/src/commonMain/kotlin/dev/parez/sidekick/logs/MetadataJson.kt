package dev.parez.sidekick.logs

// Minimal JSON encode/decode for Map<String, String> without a serialization dependency.

internal fun Map<String, String>.encodeToJson(): String = buildString {
    append('{')
    entries.forEachIndexed { i, (k, v) ->
        if (i > 0) append(',')
        append('"')
        append(k.jsonEscape())
        append("\":\"")
        append(v.jsonEscape())
        append('"')
    }
    append('}')
}

internal fun String.decodeToMetadataMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val s = trim()
    if (s == "{}" || s.isEmpty()) return result
    var i = 1 // skip '{'
    while (i < s.length - 1) {
        i = s.indexOf('"', i) + 1
        if (i == 0) break
        val keyEnd = s.nextUnescapedQuote(i)
        val key = s.substring(i, keyEnd).jsonUnescape()
        i = s.indexOf('"', keyEnd + 2) + 1
        if (i == 0) break
        val valueEnd = s.nextUnescapedQuote(i)
        val value = s.substring(i, valueEnd).jsonUnescape()
        result[key] = value
        i = valueEnd + 1
    }
    return result
}

private fun String.nextUnescapedQuote(from: Int): Int {
    var i = from
    while (i < length) {
        if (this[i] == '"' && (i == 0 || this[i - 1] != '\\')) return i
        i++
    }
    return i
}

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

private fun String.jsonUnescape(): String = replace("\\\"", "\"").replace("\\\\", "\\")
    .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
