package dev.parez.sidekick.network

/**
 * The aggregate ceiling on captured body text held by the monitor, across all recorded calls.
 *
 * `ContentLength` (in the ktor module) caps a *single* body at capture time; this caps their *sum*
 * at storage time. Once the total exceeds the budget, the oldest bodies are dropped — the calls
 * themselves are kept and flagged via [NetworkCall.bodiesEvicted], so the UI can say "dropped to
 * save memory" instead of showing an empty pane.
 *
 * This matters most on web targets, where the monitor falls back to an in-memory list held in the
 * JS heap rather than a SQLite file.
 */
public object BodyBudget {
    /** Default value: 16,777,216 characters (~16 MB of UTF-16 text). */
    public const val Default: Int = 16 * 1024 * 1024

    /** Never drop bodies. */
    public const val Unlimited: Int = Int.MAX_VALUE
}
