package dev.parez.sidekick.network.ui

import dev.parez.sidekick.network.CallStatus
import dev.parez.sidekick.network.NetworkCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkExportTest {

    private fun call(
        headers: Map<String, String> = mapOf("Accept" to "application/json"),
        body: String? = null,
        method: String = "GET",
        bodiesEvicted: Boolean = false,
    ) =
        NetworkCall(
            id = "1",
            url = "https://api.acme.com/v1/users",
            method = method,
            requestHeaders = headers,
            requestBody = body,
            requestTimestamp = 0,
            responseCode = 200,
            responseHeaders = emptyMap(),
            responseBody = null,
            responseTimestamp = 120,
            error = null,
            status = CallStatus.COMPLETE,
            bodiesEvicted = bodiesEvicted,
        )

    @Test
    fun `curl carries the method url and headers`() {
        val curl = call().toCurl()
        assertTrue(curl.startsWith("curl -X GET 'https://api.acme.com/v1/users'"))
        assertTrue(curl.contains("-H 'Accept: application/json'"))
    }

    @Test
    fun `curl includes a request body`() {
        assertTrue(call(body = """{"a":1}""").toCurl().contains("--data-raw '{\"a\":1}'"))
    }

    @Test
    fun `curl escapes single quotes so the command stays well formed`() {
        // A header value containing ' would otherwise terminate the shell string.
        val curl = call(headers = mapOf("X-Note" to "it's fine")).toCurl()
        assertTrue(curl.contains("""it'\''s fine"""), curl)
    }

    @Test
    fun `curl escapes an apostrophe in the url`() {
        // A captured URL may legitimately contain an apostrophe. Unescaped it closes
        // the shell-quoted argument, and the remainder runs as shell syntax on
        // whatever machine the exported command is pasted into.
        val curl = call().copy(url = "https://acme.com/x'; rm -rf /; echo '").toCurl()
        assertTrue(curl.contains("""x'\''; rm -rf /; echo '\''"""), curl)
    }

    @Test
    fun `curl escapes an apostrophe in a header name`() {
        val curl = call(headers = mapOf("X-It's" to "v")).toCurl()
        assertTrue(curl.contains("""X-It'\''s"""), curl)
    }

    @Test
    fun `a redacted header stays redacted in the export`() {
        val curl = call(headers = mapOf("Authorization" to "***")).toCurl()
        assertTrue(curl.contains("Authorization: ***"))
        assertFalse(curl.contains("Bearer"))
    }

    @Test
    fun `an evicted body is reported rather than silently omitted`() {
        val text = call(body = null, bodiesEvicted = true).toShareText()
        assertTrue(text.contains("dropped to save memory"), text)
    }

    @Test
    fun `the list header is pluralised`() {
        assertTrue(listOf(call()).toShareText().startsWith("Sidekick network export — 1 call\n"))
        assertTrue(
            listOf(call(), call()).toShareText().startsWith("Sidekick network export — 2 calls\n")
        )
    }

    @Test
    fun `status text maps known codes and blanks unknown ones`() {
        assertEquals("OK", statusText(200))
        assertEquals("Not Found", statusText(404))
        assertEquals("", statusText(599))
    }

    @Test
    fun `body size label switches units`() {
        assertEquals("3B", "abc".bodySizeLabel())
        assertEquals("1KB", "x".repeat(1024).bodySizeLabel())
        assertEquals("1MB", "x".repeat(1024 * 1024).bodySizeLabel())
    }

    @Test
    fun `pretty printer leaves non-json untouched`() {
        assertEquals("not json", "not json".prettyPrintJson())
    }

    @Test
    fun `pretty printer does not reformat inside strings`() {
        // Braces and commas inside a string value must survive verbatim.
        val out = """{"msg":"a, b {c}"}""".prettyPrintJson()
        assertTrue(out.contains("""a, b {c}"""), out)
    }

    @Test
    fun `url helpers split host and path`() {
        assertEquals("api.acme.com", urlHost("https://api.acme.com/v1/users?q=1"))
        assertEquals("/v1/users?q=1", urlPath("https://api.acme.com/v1/users?q=1"))
        assertEquals("", urlPath("https://api.acme.com"))
    }
}
