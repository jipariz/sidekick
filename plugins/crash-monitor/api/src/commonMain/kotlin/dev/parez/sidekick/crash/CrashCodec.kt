package dev.parez.sidekick.crash

/**
 * A line-oriented encoding for the crash log.
 *
 * Deliberately not JSON. This is written from an uncaught-exception handler while the process is
 * being torn down, which is the worst possible moment to be allocating a serializer or building a
 * document tree. One record is one `C` line followed by its `F` lines; appending is a single string
 * write, and a truncated tail costs at most the record being written — every earlier record still
 * parses.
 *
 * ```
 * C<TAB>id<TAB>timestamp<TAB>fatal<TAB>type<TAB>message<TAB>origin<TAB>thread
 * F<TAB>app|lib<TAB>frame text
 * ```
 *
 * Field values are escaped so a tab or newline inside a message cannot break the framing.
 */
internal object CrashCodec {

    fun encode(record: CrashRecord): String = buildString {
        append("C\t")
        append(record.id.esc()).append('\t')
        append(record.timestamp).append('\t')
        append(if (record.fatal) "1" else "0").append('\t')
        append(record.exceptionType.esc()).append('\t')
        append(record.message.esc()).append('\t')
        append(record.origin.esc()).append('\t')
        append(record.threadName.esc()).append('\n')
        record.frames.forEach { frame ->
            append("F\t")
            append(if (frame.isAppFrame) "app" else "lib").append('\t')
            append(frame.text.esc()).append('\n')
        }
    }

    fun decode(content: String): List<CrashRecord> {
        val records = mutableListOf<CrashRecord>()
        var current: MutableRecord? = null
        content.lineSequence().forEach { line ->
            if (line.isEmpty()) return@forEach
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                "C" -> {
                    current?.let { records += it.build() }
                    // A record truncated mid-write is dropped rather than guessed at.
                    current = if (parts.size >= 8) MutableRecord(parts) else null
                }
                "F" ->
                    if (parts.size >= 3) {
                        current?.frames?.add(StackFrame(parts[2].unesc(), parts[1] == "app"))
                    }
                else -> Unit
            }
        }
        current?.let { records += it.build() }
        return records
    }

    private class MutableRecord(private val parts: List<String>) {
        val frames = mutableListOf<StackFrame>()

        fun build() =
            CrashRecord(
                id = parts[1].unesc(),
                timestamp = parts[2].toLongOrNull() ?: 0L,
                fatal = parts[3] == "1",
                exceptionType = parts[4].unesc(),
                message = parts[5].unesc(),
                origin = parts[6].unesc(),
                threadName = parts[7].unesc(),
                frames = frames.toList(),
            )
    }

    private fun String.esc(): String =
        replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun String.unesc(): String {
        if (!contains('\\')) return this
        val out = StringBuilder(length)
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c == '\\' && i + 1 < length) {
                when (this[i + 1]) {
                    't' -> out.append('\t')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    '\\' -> out.append('\\')
                    else -> out.append(this[i + 1])
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}
