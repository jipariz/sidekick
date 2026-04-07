package dev.parez.sidekick.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryPreferenceStore : PreferenceStore {
    private val data = HashMap<String, MutableStateFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T> =
        data.getOrPut(key) { MutableStateFlow(defaultValue) } as StateFlow<T>

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> set(key: String, value: T) {
        (data.getOrPut(key) { MutableStateFlow(value) } as MutableStateFlow<T>).value = value
    }
}
