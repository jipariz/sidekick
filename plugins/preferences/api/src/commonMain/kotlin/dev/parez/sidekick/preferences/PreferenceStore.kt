package dev.parez.sidekick.preferences

import kotlinx.coroutines.flow.StateFlow

public interface PreferenceStore {
    public fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T>

    public suspend fun <T : Any> set(key: String, value: T)
}

public expect fun createPreferenceStore(storeName: String): PreferenceStore
