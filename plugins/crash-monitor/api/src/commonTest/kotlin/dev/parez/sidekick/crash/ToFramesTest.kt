package dev.parez.sidekick.crash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `isAppFrame` is what makes a trace scannable on a phone — if it stops distinguishing app frames
 * the detail pane degrades to an undifferentiated wall of text.
 */
class ToFramesTest {

    private val throwable = IllegalStateException("boom")

    @Test
    fun `every returned line is a stack frame`() {
        // The JVM prefixes a trace with "<type>: <message>"; Kotlin/Native does not.
        // Either way nothing but frames should come back — and on Native the top
        // frame must survive, which an unconditional drop(1) used to destroy.
        val frames = throwable.toFrames(appPackagePrefix = null)
        assertTrue(frames.isNotEmpty())
        assertTrue(
            frames.all { it.text.startsWith("at ") },
            "non-frame line returned: ${frames.firstOrNull { !it.text.startsWith("at ") }?.text}",
        )
    }

    @Test
    fun `the header line is not returned as a frame`() {
        val frames = throwable.toFrames(appPackagePrefix = null)
        assertTrue(frames.none { it.text.startsWith("java.lang.") })
        assertTrue(frames.none { it.text == "IllegalStateException: boom" })
    }

    @Test
    fun `produces at least one frame for a thrown exception`() {
        assertTrue(throwable.toFrames(appPackagePrefix = null).isNotEmpty())
    }

    @Test
    fun `marks nothing as an app frame when no prefix is supplied`() {
        assertTrue(throwable.toFrames(appPackagePrefix = null).none { it.isAppFrame })
    }

    @Test
    fun `marks frames containing the prefix`() {
        // This test class is in dev.parez.sidekick.crash, so its own frames match.
        val frames = throwable.toFrames(appPackagePrefix = "dev.parez.sidekick.crash")
        assertTrue(frames.any { it.isAppFrame }, frames.joinToString("\n") { it.text })
        assertTrue(frames.filter { it.isAppFrame }.all { it.text.contains("dev.parez.sidekick") })
    }

    @Test
    fun `a prefix that matches nothing leaves every frame as library code`() {
        assertEquals(
            emptyList(),
            throwable.toFrames(appPackagePrefix = "com.nonexistent.app").filter { it.isAppFrame },
        )
    }

    @Test
    fun `frames are trimmed and never blank`() {
        val frames = throwable.toFrames(appPackagePrefix = null)
        assertTrue(frames.none { it.text.isBlank() })
        assertTrue(frames.none { it.text != it.text.trim() })
    }
}
