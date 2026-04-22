package dev.parez.sidekick.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryPreferenceStore : PreferenceStore {
    private val data = HashMap<String, MutableStateFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T> {
        val existing = data[key]
        if (existing != null) {
            val currentValue = existing.value
            check(currentValue::class == defaultValue::class) {
                "InMemoryPreferenceStore key '$key' already holds a ${currentValue::class.simpleName} " +
                    "but was accessed as ${defaultValue::class.simpleName}"
            }
            return existing as StateFlow<T>
        }
        return data.getOrPut(key) { MutableStateFlow(defaultValue) } as StateFlow<T>
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> set(key: String, value: T) {
        val existing = data[key]
        if (existing != null) {
            check(existing.value::class == value::class) {
                "InMemoryPreferenceStore key '$key' already holds a ${existing.value::class.simpleName} " +
                    "but set() was called with ${value::class.simpleName}"
            }
            (existing as MutableStateFlow<T>).value = value
        } else {
            data[key] = MutableStateFlow(value)
        }
    }
}
