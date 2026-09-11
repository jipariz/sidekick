package dev.parez.sidekick.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafeFileNameTest {

    @Test
    fun `keeps characters that are legal on every platform`() {
        assertEquals("sidekick-network", "sidekick-network".toSafeFileName())
        assertEquals("a.b_c-1", "a.b_c-1".toSafeFileName())
    }

    @Test
    fun `replaces path separators so a subject cannot escape the chosen directory`() {
        assertEquals("a_b", "a/b".toSafeFileName())
        assertEquals("a_b", "a\\b".toSafeFileName())
        assertTrue("../../etc/passwd".toSafeFileName().none { it == '/' })
    }

    @Test
    fun `replaces spaces and punctuation`() {
        assertEquals("my_export", "my export".toSafeFileName())
        assertEquals("a_b", "a:b".toSafeFileName())
    }

    @Test
    fun `trims the underscores a replacement can leave at the edges`() {
        assertEquals("x", " x ".toSafeFileName())
        assertEquals("x", "///x///".toSafeFileName())
    }

    @Test
    fun `falls back when nothing usable survives`() {
        assertEquals("sidekick", "".toSafeFileName())
        assertEquals("sidekick", "   ".toSafeFileName())
        assertEquals("sidekick", "///".toSafeFileName())
    }
}
