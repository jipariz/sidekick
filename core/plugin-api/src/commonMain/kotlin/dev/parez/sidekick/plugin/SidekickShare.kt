package dev.parez.sidekick.plugin

/**
 * Hands a block of text to the platform's sharing mechanism.
 *
 * Copy-to-clipboard is enough to move a single value around, but it cannot get a log dump or a
 * captured request off a device. This can: an intent chooser on Android, a share sheet on iOS, a
 * file on desktop, a download in the browser.
 *
 * Best-effort by design — every implementation swallows failures rather than throwing into a debug
 * overlay. Call from the main thread.
 *
 * @param text the payload. Plain text; callers format it.
 * @param subject a short title. Used as the chooser title on Android, the file name on desktop and
 *   web (sanitized), and ignored on iOS.
 */
public expect object SidekickShare {
    public fun share(text: String, subject: String)
}

/**
 * Reduces [subject] to something safe to use as a file name on any platform. Falls back to
 * `"sidekick"` when nothing usable survives.
 */
internal fun String.toSafeFileName(): String =
    replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "sidekick" }
