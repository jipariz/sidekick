package dev.parez.sidekick.plugin

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement

public actual object SidekickShare {
    /**
     * Browsers have no synchronous share primitive that works everywhere (`navigator.share` is
     * absent on desktop Firefox and requires a secure context), so this downloads the payload as a
     * text file instead — the outcome a developer actually wants from an overlay export.
     *
     * Uses a `data:` URL rather than a Blob so the whole implementation stays in shared `webMain`
     * code: Blob construction differs between the JS and Wasm interop models, percent-encoding does
     * not.
     */
    public actual fun share(text: String, subject: String) {
        runCatching {
            val anchor = document.createElement("a") as HTMLAnchorElement
            anchor.href = "data:text/plain;charset=utf-8,${text.percentEncode()}"
            anchor.setAttribute("download", "${subject.toSafeFileName()}.txt")
            anchor.style.display = "none"
            val body = document.body ?: return
            body.appendChild(anchor)
            anchor.click()
            body.removeChild(anchor)
        }
    }
}

private const val HEX = "0123456789ABCDEF"

/**
 * Percent-encodes UTF-8 bytes. Hand-rolled rather than calling `encodeURIComponent`, which would
 * need per-target `js(...)` interop and split this file in two.
 */
private fun String.percentEncode(): String {
    val out = StringBuilder(length)
    for (byte in encodeToByteArray()) {
        val value = byte.toInt() and 0xFF
        val char = value.toChar()
        if (char.isUnreservedUrlChar()) {
            out.append(char)
        } else {
            out.append('%').append(HEX[value shr 4]).append(HEX[value and 0x0F])
        }
    }
    return out.toString()
}

private fun Char.isUnreservedUrlChar(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in "-_.!~*'()"
