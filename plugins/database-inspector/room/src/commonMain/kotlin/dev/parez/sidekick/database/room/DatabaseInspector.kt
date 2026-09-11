package dev.parez.sidekick.database.room

import androidx.room3.RoomDatabase

/**
 * Binds a Room database to the Sidekick Database panel.
 *
 * ```kotlin
 * DatabaseInspector.attach(database, fileName = "app.db")
 * ```
 *
 * Tables are read on attach, again whenever the panel is opened, and on Refresh. Cell edits write
 * back through Room's writer connection; ad-hoc SQL runs read-only against the reader connection.
 *
 * On JS and WasmJS this marks the panel unsupported instead of attaching — see the `webMain` actual
 * of [attachRoomDatabase].
 */
public object DatabaseInspector {
    public fun attach(database: RoomDatabase, fileName: String? = null) {
        attachRoomDatabase(database, fileName)
    }
}

/**
 * Platform half of [DatabaseInspector.attach].
 *
 * Split by source set rather than by a runtime flag because the implementation calls into Room's
 * pooled-connection API, whose `SQLiteStatement` lambda types are not on the common metadata
 * compile classpath — and because web has no working implementation to offer regardless.
 */
internal expect fun attachRoomDatabase(database: RoomDatabase, fileName: String?)

/**
 * Why web is excluded — deliberately, not incidentally.
 *
 * A generic table browser reads arbitrary columns of unknown nullability, and
 * `androidx.sqlite:sqlite-web`'s worker samples `sqlite3_column_type` from the *first row of a
 * statement* and reports that type for every subsequent row. A column that is non-null in row 0 and
 * NULL later would be read with the wrong typed getter and throw inside the JS bridge. The monitors
 * dodge this by having no nullable columns; a browser over the consumer's arbitrary schema has no
 * such luxury.
 */
internal const val WEB_UNSUPPORTED_REASON: String =
    "Database inspection is not available on web builds. The sqlite-web worker reports a " +
        "column's type from the first row only, so reading a schema with nullable columns is " +
        "unsafe. Android, desktop and iOS builds are unaffected."
