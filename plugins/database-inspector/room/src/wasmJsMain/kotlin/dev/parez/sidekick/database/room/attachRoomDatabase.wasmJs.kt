package dev.parez.sidekick.database.room

import androidx.room3.RoomDatabase
import dev.parez.sidekick.database.di.DatabaseInspectorKoinContext

@Suppress("UNUSED_PARAMETER")
internal actual fun attachRoomDatabase(database: RoomDatabase, fileName: String?) {
    DatabaseInspectorKoinContext.getDefaultStore().markUnsupported(WEB_UNSUPPORTED_REASON)
}
