package dev.parez.sidekick.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.parez.sidekick.plugin.ApplicationContextHolder
import kotlinx.coroutines.flow.first

private val key = stringPreferencesKey(MENU_ORDER_KEY)

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = {
            ApplicationContextHolder.context.preferencesDataStoreFile(MENU_ORDER_STORE_NAME)
        }
    )
}

private object AndroidMenuOrderStore : MenuOrderStore {
    override suspend fun read(): List<String> =
        decodeMenuOrder(dataStore.data.first()[key])

    override suspend fun write(ids: List<String>) {
        dataStore.edit { prefs -> prefs[key] = encodeMenuOrder(ids) }
    }
}

internal actual fun createMenuOrderStore(): MenuOrderStore = AndroidMenuOrderStore
