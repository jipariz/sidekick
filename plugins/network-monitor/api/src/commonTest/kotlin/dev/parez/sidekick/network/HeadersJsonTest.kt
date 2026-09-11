package dev.parez.sidekick.network

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Hand-rolled JSON, written to avoid a serialization dependency. Header values are arbitrary
 * server-controlled strings, so the escaping is the part that has to hold.
 */
class HeadersJsonTest {

    private fun roundTrip(map: Map<String, String>) =
        assertEquals(map, map.encodeToJson().decodeToHeaderMap())

    @Test fun `round trips an empty map`() = roundTrip(emptyMap())

    @Test fun `round trips a single entry`() = roundTrip(mapOf("Accept" to "application/json"))

    @Test
    fun `round trips several entries`() =
        roundTrip(
            mapOf(
                "Accept" to "application/json",
                "Content-Type" to "text/plain; charset=utf-8",
                "X-Request-Id" to "abc-123",
            )
        )

    @Test
    fun `round trips quotes and backslashes in a value`() =
        roundTrip(mapOf("X-Weird" to """he said "hi" and \escaped\"""))

    @Test fun `round trips a newline in a value`() = roundTrip(mapOf("X-Multi" to "one\ntwo"))

    @Test fun `round trips an empty value`() = roundTrip(mapOf("X-Empty" to ""))

    @Test
    fun `round trips a redaction placeholder`() =
        roundTrip(mapOf("Authorization" to "***", "Cookie" to "***"))

    @Test
    fun `a value ending in a backslash does not swallow the entries after it`() {
        // Regression: the decoder treated any quote preceded by a backslash as
        // escaped, so `...\\"` — an escaped backslash then the real terminator —
        // ran past the end of the value and consumed the rest of the object.
        val map = mapOf("X-Path" to "C:\\dir\\", "X-After" to "intact")
        assertEquals(map, map.encodeToJson().decodeToHeaderMap())
    }

    @Test
    fun `decodes an empty object to an empty map`() {
        assertEquals(emptyMap(), "{}".decodeToHeaderMap())
        assertEquals(emptyMap(), "".decodeToHeaderMap())
    }
}
