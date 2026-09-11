# Database Inspector

Browse and edit your app's SQLite database from inside the running app: schema, contents, single-cell edits, and a read-only SQL console.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)

Web builds compile and show the panel, but it renders an unsupported state. See [Why not web](#why-not-web).

## Features

- **Schema browser** — every table, its columns and declared types, and a row count.
- **Contents** — up to 500 rows per table, rendered by actual SQLite type rather than declared affinity.
- **Cell editing** — tap a cell to change it, or set it to `NULL`. Writes go through Room's writer connection and wake your own Flows, so your app's UI updates too.
- **SQL console** — `SELECT` / `PRAGMA` / `EXPLAIN` against the live database.
- **CSV export** — share any table through the platform share sheet.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:database-inspector:api` | Models, `DatabaseController` contract, store. |
| `:plugins:database-inspector:ui` | `DatabaseInspectorPlugin` and its screens. |
| `:plugins:database-inspector:room` | Binds a Room database to the panel. |
| `:plugins:database-inspector:noop` | Release stub. Same API, nothing opened. |

## Setup

### 1. Add dependencies

```kotlin
commonMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:2026.09.11"))
    implementation("dev.parez.sidekick:database-inspector-ui")
    implementation("dev.parez.sidekick:database-inspector-room")
}
```

### 2. Register the plugin

```kotlin
val databasePlugin = remember { DatabaseInspectorPlugin() }

Sidekick(plugins = listOf(databasePlugin, /* … */))
```

### 3. Attach your database

```kotlin
DatabaseInspector.attach(database, fileName = "app.db")
```

Attach once, wherever you build the database. Sidekick does not discover databases on its own; auto-discovery would also pick up Sidekick's own monitor databases.

## Editing rules

Cell editing needs an addressable row, so each table is read as `SELECT rowid AS _sk_rowid, * FROM t`. Tables declared `WITHOUT ROWID` have no such column and are shown read-only. Query results are always read-only, since a projection has no rows to address.

Edited values are bound as text and converted by the column's affinity, matching how SQLite treats the same literal in a hand-written `UPDATE`.

## The SQL console is an allowlist

Only `SELECT`, `PRAGMA` and `EXPLAIN` run, and only one statement at a time.

`WITH` is rejected because SQLite permits a CTE to precede `INSERT`, `UPDATE` or `DELETE`, so `WITH x AS (…) DELETE FROM t` would pass a keyword check. Inline the CTE as a subquery instead.

Cell editing is the only write path.

## Why not web

`androidx.sqlite:sqlite-web`'s worker samples `sqlite3_column_type` from the first row of a statement and reports that type for every subsequent row. A column that is non-null in row 0 and `NULL` later is read with the wrong typed getter and throws inside the JS bridge.

Sidekick's monitors avoid this by using no nullable columns. A browser over an arbitrary schema cannot, so on JS and WasmJS the panel explains the limitation instead of failing at runtime. Android, desktop and iOS are unaffected.

## Release builds

Swap `database-inspector-ui` and `database-inspector-room` for `database-inspector-noop`. See [Release Builds](../release-builds.md). In the noop, `attach()` accepts the database and ignores it: no connection is opened and no schema is read.
