package dev.parez.sidekick.crash

/**
 * Where the crash log lives between runs.
 *
 * [append] is called from an uncaught-exception handler, so it must be synchronous and must not
 * depend on coroutines, Room, or anything else that may already be shutting down. Every
 * implementation swallows its own failures: losing the record is bad, but replacing the app's real
 * crash with an I/O exception from the crash reporter is worse.
 */
internal expect object CrashStorage {
    fun read(): String?

    fun append(text: String)

    fun clear()
}

/** File name / storage key used by every platform implementation. */
internal const val CRASH_LOG_NAME: String = "sidekick_crashes.log"

/**
 * Records kept across restarts. Crash volumes are small and read whole, so this is a count rather
 * than a byte budget.
 */
internal const val MAX_CRASHES: Int = 50
