package dev.parez.sidekick.database.room

import androidx.room3.RoomDatabase

/**
 * Release-variant stub for `DatabaseInspector`. Accepts a database and ignores it — no connection
 * is opened and no schema is read.
 */
public object DatabaseInspector {
    @Suppress("UNUSED_PARAMETER")
    public fun attach(database: RoomDatabase, fileName: String? = null): Unit = Unit
}
