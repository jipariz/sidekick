package dev.parez.sidekick.network.ui.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.parez.sidekick.plugin.ApplicationContextHolder
import kotlinx.coroutines.flow.first

private val key = stringPreferencesKey(PANE_SIZE_KEY)

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = {
            ApplicationContextHolder.context.preferencesDataStoreFile(PANE_SIZE_STORE_NAME)
        }
    )
}

private object AndroidPaneSizeStore : PaneSizeStore {
    override suspend fun read(): NetworkMonitorPaneSizes? =
        decodePaneSizes(dataStore.data.first()[key])

    override suspend fun write(sizes: NetworkMonitorPaneSizes) {
        dataStore.edit { prefs -> prefs[key] = encodePaneSizes(sizes) }
    }
}

internal actual fun createPaneSizeStore(): PaneSizeStore = AndroidPaneSizeStore
