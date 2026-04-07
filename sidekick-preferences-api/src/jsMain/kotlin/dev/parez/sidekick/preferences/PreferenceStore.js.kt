package dev.parez.sidekick.preferences

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun createPreferenceStore(storeName: String): PreferenceStore =
    LocalStoragePreferenceStore(storeName)

class LocalStoragePreferenceStore(private val storeName: String) : PreferenceStore {
    private val cache = HashMap<String, MutableStateFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T> {
        return cache.getOrPut(key) {
            val stored = localStorage.getItem("$storeName.$key")
            val initial: Any = if (stored != null) {
                when (defaultValue) {
                    is Boolean -> stored.toBoolean()
                    is String  -> stored
                    is Int     -> stored.toIntOrNull()    ?: defaultValue
                    is Long    -> stored.toLongOrNull()   ?: defaultValue
                    is Float   -> stored.toFloatOrNull()  ?: defaultValue
                    is Double  -> stored.toDoubleOrNull() ?: defaultValue
                    else       -> defaultValue
                }
            } else {
                defaultValue
            }
            MutableStateFlow(initial)
        } as StateFlow<T>
    }

    override suspend fun <T : Any> set(key: String, value: T) {
        localStorage.setItem("$storeName.$key", value.toString())
        @Suppress("UNCHECKED_CAST")
        (cache[key] as? MutableStateFlow<T>)?.value = value
    }
}
