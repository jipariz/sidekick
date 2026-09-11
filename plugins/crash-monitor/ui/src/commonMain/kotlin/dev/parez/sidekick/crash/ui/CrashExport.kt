package dev.parez.sidekick.crash.ui

import dev.parez.sidekick.crash.CrashRecord

internal fun CrashRecord.toShareText(): String = buildString {
    appendLine(title)
    appendLine("${if (fatal) "Fatal" else "Non-fatal"} · $origin · thread $threadName")
    appendLine()
    frames.forEach { frame -> appendLine("    ${frame.text}") }
}

internal fun List<CrashRecord>.toShareText(): String = buildString {
    appendLine("Sidekick crash export — ${this@toShareText.size} records")
    this@toShareText.asReversed().forEach { crash ->
        appendLine()
        append(crash.toShareText())
    }
}
