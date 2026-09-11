package dev.parez.sidekick.crash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashCodecTest {

    private fun record(
        id: String = "abc",
        fatal: Boolean = true,
        type: String = "IllegalStateException",
        message: String = "boom",
        origin: String = "uncaught",
        thread: String = "main",
        frames: List<StackFrame> = listOf(StackFrame("at com.acme.Foo.bar(Foo.kt:12)", true)),
    ) =
        CrashRecord(
            id = id,
            timestamp = 1_700_000_000_000L,
            fatal = fatal,
            exceptionType = type,
            message = message,
            origin = origin,
            threadName = thread,
            frames = frames,
        )

    @Test
    fun `round trips a record`() {
        val original = record()
        val decoded = CrashCodec.decode(CrashCodec.encode(original))
        assertEquals(listOf(original), decoded)
    }

    @Test
    fun `round trips multiple appended records`() {
        val first = record(id = "1", fatal = true)
        val second = record(id = "2", fatal = false, type = "IOException")
        val log = CrashCodec.encode(first) + CrashCodec.encode(second)
        assertEquals(listOf(first, second), CrashCodec.decode(log))
    }

    @Test
    fun `preserves tabs and newlines in the message`() {
        // The format is tab-delimited and line-oriented, so these are exactly the
        // characters that would break framing if escaping regressed.
        val original = record(message = "line one\nline\ttwo\r\\end")
        val decoded = CrashCodec.decode(CrashCodec.encode(original)).single()
        assertEquals("line one\nline\ttwo\r\\end", decoded.message)
    }

    @Test
    fun `preserves app-frame flags and frame order`() {
        val frames =
            listOf(
                StackFrame("at com.acme.Foo.bar(Foo.kt:12)", isAppFrame = true),
                StackFrame("at kotlin.Thing.run(Thing.kt:1)", isAppFrame = false),
                StackFrame("at com.acme.Baz.qux(Baz.kt:9)", isAppFrame = true),
            )
        val decoded = CrashCodec.decode(CrashCodec.encode(record(frames = frames))).single()
        assertEquals(frames, decoded.frames)
    }

    @Test
    fun `a record truncated mid-write does not lose the records before it`() {
        // The whole reason for the line format: a process dying part-way through an
        // append must cost at most the record being written.
        val complete = record(id = "1")
        val log = CrashCodec.encode(complete) + "C\t2\t1700000000000\t1\tIllegal"
        val decoded = CrashCodec.decode(log)
        assertEquals(listOf(complete), decoded)
    }

    @Test
    fun `ignores unrecognised lines rather than throwing`() {
        val log = "garbage\n" + CrashCodec.encode(record()) + "\nmore garbage\n"
        assertEquals(1, CrashCodec.decode(log).size)
    }

    @Test
    fun `decodes an empty log to no records`() {
        assertEquals(emptyList(), CrashCodec.decode(""))
    }

    @Test
    fun `frames without a preceding record are dropped`() {
        assertTrue(CrashCodec.decode("F\tapp\torphan frame\n").isEmpty())
    }

    @Test
    fun `round trips a record with no frames and a blank message`() {
        val original = record(message = "", frames = emptyList())
        assertEquals(listOf(original), CrashCodec.decode(CrashCodec.encode(original)))
    }
}
