package dev.parez.sidekick.crash.ui

import dev.parez.sidekick.crash.CrashRecord
import dev.parez.sidekick.crash.StackFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashExportTest {

    private fun record(id: String = "1", fatal: Boolean = true, message: String = "boom") =
        CrashRecord(
            id = id,
            timestamp = 0L,
            fatal = fatal,
            exceptionType = "IllegalStateException",
            message = message,
            origin = "uncaught",
            threadName = "main",
            frames = listOf(StackFrame("at com.acme.Foo.bar(Foo.kt:12)", true)),
        )

    @Test
    fun `a single record carries type message severity and frames`() {
        val text = record().toShareText()
        assertTrue(text.contains("IllegalStateException: boom"))
        assertTrue(text.contains("Fatal"))
        assertTrue(text.contains("at com.acme.Foo.bar(Foo.kt:12)"))
    }

    @Test
    fun `a non-fatal is labelled as such`() {
        assertTrue(record(fatal = false).toShareText().contains("Non-fatal"))
    }

    @Test
    fun `the list header is pluralised`() {
        assertTrue(listOf(record()).toShareText().startsWith("Sidekick crash export — 1 record\n"))
        assertTrue(
            listOf(record("1"), record("2"))
                .toShareText()
                .startsWith("Sidekick crash export — 2 records\n")
        )
    }

    @Test
    fun `the list is newest first`() {
        // The store appends, so the export has to reverse — the crash you are
        // chasing is the one that just happened.
        val text =
            listOf(record("old", message = "older"), record("new", message = "newest"))
                .toShareText()
        assertTrue(text.indexOf("newest") < text.indexOf("older"), text)
    }

    @Test
    fun `a blank message falls back to the exception type alone`() {
        assertEquals("IllegalStateException", record(message = "").title)
    }
}
