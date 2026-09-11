package dev.parez.sidekick.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkFilterTest {

    private fun call(url: String = "https://api.acme.com/v1/users", method: String = "GET") =
        NetworkCall(
            id = "1",
            url = url,
            method = method,
            requestHeaders = emptyMap(),
            requestBody = null,
            requestTimestamp = 0,
            responseCode = 200,
            responseHeaders = emptyMap(),
            responseBody = null,
            responseTimestamp = 1,
            error = null,
            status = CallStatus.COMPLETE,
        )

    @Test
    fun `an empty filter matches everything`() {
        assertTrue(NetworkFilter().matches(call()))
    }

    @Test
    fun `query matches url and method case-insensitively`() {
        assertTrue(NetworkFilter(query = "acme").matches(call()))
        assertTrue(NetworkFilter(query = "ACME").matches(call()))
        assertTrue(NetworkFilter(query = "get").matches(call()))
        assertFalse(NetworkFilter(query = "nope").matches(call()))
    }

    @Test
    fun `method filter is exact and case-insensitive on the call side`() {
        assertTrue(NetworkFilter(methods = setOf("GET")).matches(call(method = "get")))
        assertFalse(NetworkFilter(methods = setOf("POST")).matches(call(method = "GET")))
    }

    @Test
    fun `query and method filters combine`() {
        val filter = NetworkFilter(query = "users", methods = setOf("GET"))
        assertTrue(filter.matches(call()))
        assertFalse(filter.matches(call(method = "POST")))
        assertFalse(filter.matches(call(url = "https://api.acme.com/v1/orders")))
    }

    @Test
    fun `a blank query becomes a match-all LIKE token`() {
        assertEquals("%", NetworkFilter().toLikeToken())
        assertEquals("%", NetworkFilter(query = "   ").toLikeToken())
    }

    @Test
    fun `LIKE wildcards in the query are escaped so they match literally`() {
        // Without this a user searching for "100%" would match every row.
        assertEquals("%100\\%%", NetworkFilter(query = "100%").toLikeToken())
        assertEquals("%a\\_b%", NetworkFilter(query = "a_b").toLikeToken())
        assertEquals("%a\\\\b%", NetworkFilter(query = "a\\b").toLikeToken())
    }
}
