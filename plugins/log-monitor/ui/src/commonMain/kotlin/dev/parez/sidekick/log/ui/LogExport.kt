package dev.parez.sidekick.log.ui

import dev.parez.sidekick.log.LogEntry

/** Full dump of a single entry, including the stack trace and any metadata. */
internal fun LogEntry.toShareText(): String = buildString {
    appendLine("${level.name.first()}/$tag: $message")
    if (!metadata.isNullOrEmpty()) {
        appendLine()
        appendLine("── Metadata ──")
        metadata?.forEach { (key, value) -> appendLine("$key: $value") }
    }
    throwable?.let {
        appendLine()
        appendLine("── Stack trace ──")
        appendLine(it)
    }
}

/**
 * Logcat-shaped one line per entry, newest first. Stack traces are indented under their entry so a
 * multi-line throwable stays visually attached to the line that produced it.
 */
internal fun List<LogEntry>.toShareText(): String = buildString {
    val count = this@toShareText.size
    appendLine("Sidekick log export — $count ${if (count == 1) "entry" else "entries"}")
    appendLine()
    this@toShareText.forEach { entry ->
        appendLine("${entry.level.name.first()}/${entry.tag}: ${entry.message}")
        entry.throwable?.lineSequence()?.forEach { line -> appendLine("    $line") }
    }
}
