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
    /**
     * Default value: 8,388,608 characters.
     *
     * The budget counts *characters*, and a Kotlin `String` stores each as UTF-16, so this is the
     * ~16 MiB of retained memory the ceiling is meant to express — before string and collection
     * overhead. The previous 16,777,216 was ~32 MiB, which quietly doubled the limit it documented.
     */
    public const val Default: Int = 8 * 1024 * 1024

    /** Never drop bodies. */
    public const val Unlimited: Int = Int.MAX_VALUE
}
