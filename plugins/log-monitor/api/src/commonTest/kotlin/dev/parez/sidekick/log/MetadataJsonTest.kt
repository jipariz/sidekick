package dev.parez.sidekick.log

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataJsonTest {

    private fun roundTrip(map: Map<String, String>) =
        assertEquals(map, map.encodeToJson().decodeToMetadataMap())

    @Test fun `round trips an empty map`() = roundTrip(emptyMap())

    @Test fun `round trips several entries`() = roundTrip(mapOf("a" to "1", "b" to "2"))

    @Test fun `round trips quotes and backslashes`() = roundTrip(mapOf("k" to """a "b" c\d"""))

    @Test fun `round trips a newline`() = roundTrip(mapOf("stack" to "line1\nline2"))

    @Test
    fun `a value ending in a backslash does not swallow the entries after it`() {
        // Same regression as the network monitor's header codec — the two decoders
        // are the same hand-rolled parser.
        val map = mapOf("path" to """C:\dir\""", "after" to "intact")
        roundTrip(map)
    }

    @Test
    fun `decodes an empty object to an empty map`() {
        assertEquals(emptyMap(), "{}".decodeToMetadataMap())
        assertEquals(emptyMap(), "".decodeToMetadataMap())
    }
}
