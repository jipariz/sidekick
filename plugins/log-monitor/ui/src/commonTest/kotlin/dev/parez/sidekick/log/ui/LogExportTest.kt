package dev.parez.sidekick.log.ui

import dev.parez.sidekick.log.LogEntry
import dev.parez.sidekick.log.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogExportTest {

    private fun entry(
        id: String = "1",
        level: LogLevel = LogLevel.INFO,
        message: String = "hello",
        throwable: String? = null,
        metadata: Map<String, String>? = null,
    ) =
        LogEntry(
            id = id,
            timestamp = 0L,
            level = level,
            tag = "Tag",
            message = message,
            throwable = throwable,
            metadata = metadata,
        )

    @Test
    fun `a single entry is rendered logcat style`() {
        assertTrue(entry().toShareText().startsWith("I/Tag: hello"))
        assertTrue(entry(level = LogLevel.ERROR).toShareText().startsWith("E/Tag: hello"))
    }

    @Test
    fun `metadata and stack trace are included when present`() {
        val text =
            entry(throwable = "java.lang.Error\n\tat Foo.kt:1", metadata = mapOf("k" to "v"))
                .toShareText()
        assertTrue(text.contains("k: v"))
        assertTrue(text.contains("at Foo.kt:1"))
    }

    @Test
    fun `the list header is pluralised`() {
        assertTrue(listOf(entry()).toShareText().startsWith("Sidekick log export — 1 entry\n"))
        assertTrue(
            listOf(entry("1"), entry("2"))
                .toShareText()
                .startsWith("Sidekick log export — 2 entries\n")
        )
    }

    @Test
    fun `a multi-line stack trace is indented under its entry`() {
        val text = listOf(entry(throwable = "java.lang.Error\n\tat Foo.kt:1")).toShareText()
        assertTrue(text.lines().any { it.startsWith("    ") }, text)
    }

    @Test
    fun `level letters are distinct`() {
        val letters = LogLevel.entries.map { entry(level = it).toShareText().first() }
        assertEquals(
            letters.size,
            letters.toSet().size + if (letters.size > letters.toSet().size) 0 else 0,
        )
        assertTrue(letters.contains('V') && letters.contains('E'))
    }
}
