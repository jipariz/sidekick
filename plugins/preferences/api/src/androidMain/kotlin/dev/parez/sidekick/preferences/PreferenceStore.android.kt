package dev.parez.sidekick.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

actual fun createPreferenceStore(storeName: String): PreferenceStore {
    val context = ApplicationContextHolder.context
    val dataStore = stores.getOrPut(storeName) {
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(storeName) }
        )
    }
    return DataStorePreferenceStore(dataStore)
}

class DataStorePreferenceStore(private val dataStore: DataStore<Preferences>) : PreferenceStore {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cache = ConcurrentHashMap<String, MutableStateFlow<Any>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: String, defaultValue: T): StateFlow<T> {
        val flow = cache.getOrPut(key) {
            val f = MutableStateFlow<Any>(defaultValue)
            scope.launch {
                dataStore.data.collect { prefs ->
                    f.value = when (defaultValue) {
                        is Boolean -> prefs[booleanPreferencesKey(key)] ?: defaultValue
                        is String  -> prefs[stringPreferencesKey(key)]  ?: defaultValue
                        is Int     -> prefs[intPreferencesKey(key)]     ?: defaultValue
                        is Long    -> prefs[longPreferencesKey(key)]    ?: defaultValue
                        is Float   -> prefs[floatPreferencesKey(key)]   ?: defaultValue
                        is Double  -> prefs[doublePreferencesKey(key)]  ?: defaultValue
                        else       -> defaultValue
                    }
                }
            }
            f
        }
        return flow as StateFlow<T>
    }

    override suspend fun <T : Any> set(key: String, value: T) {
        dataStore.edit { prefs ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(key)] = value
                is String  -> prefs[stringPreferencesKey(key)]  = value
                is Int     -> prefs[intPreferencesKey(key)]     = value
                is Long    -> prefs[longPreferencesKey(key)]    = value
                is Float   -> prefs[floatPreferencesKey(key)]   = value
                is Double  -> prefs[doublePreferencesKey(key)]  = value
            }
        }
    }
}
