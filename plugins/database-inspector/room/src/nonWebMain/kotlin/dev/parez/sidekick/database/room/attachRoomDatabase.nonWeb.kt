package dev.parez.sidekick.database.room

import androidx.room3.RoomDatabase
import dev.parez.sidekick.database.di.DatabaseInspectorKoinContext

internal actual fun attachRoomDatabase(database: RoomDatabase, fileName: String?) {
    val store = DatabaseInspectorKoinContext.getDefaultStore()
    store.attach(RoomDatabaseController(database, fileName, store))
}
