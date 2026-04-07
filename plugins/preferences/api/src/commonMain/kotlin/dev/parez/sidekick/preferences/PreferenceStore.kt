package dev.parez.sidekick.preferences

import kotlinx.coroutines.flow.StateFlow

interface PreferenceStore {
    fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T>
    suspend fun <T : Any> set(key: String, value: T)
}

expect fun createPreferenceStore(storeName: String): PreferenceStore
