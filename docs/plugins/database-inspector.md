# Database Inspector

Browse and edit your app's own SQLite database from inside the running app — schema, contents, single-cell edits, and a read-only SQL console. No debugger, no cable, no `adb pull`.

## Platforms

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white)
![Desktop](https://img.shields.io/badge/Desktop_(JVM)-4E8EE9?logo=openjdk&logoColor=white)

Web builds compile and show the panel, but it renders an explanatory unsupported state — see [Why not web](#why-not-web).

## Features

- **Schema browser** — every table, its columns and declared types, and a row count.
- **Contents** — up to 500 rows per table, values rendered by their *actual* SQLite type rather than declared affinity.
- **Cell editing** — tap a cell to change it, or set it to `NULL`. Writes go through Room's writer connection and then wake your own Flows, so your app's UI updates too.
- **SQL console** — `SELECT` / `PRAGMA` / `EXPLAIN` against the live database.
- **CSV export** — share any table through the platform share sheet.

## Modules

| Module | Purpose |
|---|---|
| `:plugins:database-inspector:api` | Models, `DatabaseController` contract, store. |
| `:plugins:database-inspector:ui` | `DatabaseInspectorPlugin` and its screens. |
| `:plugins:database-inspector:room` | Binds a Room database to the panel. |
| `:plugins:database-inspector:noop` | Release stub — same API, nothing opened. |

## Setup

### 1. Add dependencies

```kotlin
commonMain.dependencies {
    implementation(platform("dev.parez.sidekick:bom:<bom-version>"))
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

Attach once, wherever you build the database. Sidekick does **not** go looking for database files on its own — an overlay that guesses would also find Sidekick's own monitor databases sitting next to yours.

## Editing rules

Cell editing needs an addressable row, so each table is read as `SELECT rowid AS _sk_rowid, * FROM t`. Tables declared `WITHOUT ROWID` have no such column; they are shown read-only and the panel says so. Query results are always read-only — a projection has no rows to address.

Edited values are bound as text and converted by the column's affinity, which is how SQLite would treat the same literal in a hand-written `UPDATE`.

## The SQL console is an allowlist

Only `SELECT`, `PRAGMA` and `EXPLAIN` run, and only one statement at a time.

`WITH` is rejected on purpose: SQLite permits a CTE to precede `INSERT` / `UPDATE` / `DELETE`, so `WITH x AS (…) DELETE FROM t` would pass a naive keyword check. Inline the CTE as a subquery instead.

A debug overlay that will happily drop a table because you mistyped is not a feature. Cell editing is the deliberate, narrow write path.

## Why not web

`androidx.sqlite:sqlite-web`'s worker samples `sqlite3_column_type` from the **first row of a statement** and reports that type for every subsequent row. A column that is non-null in row 0 and `NULL` later would be read with the wrong typed getter and throw inside the JS bridge.

Sidekick's own monitors avoid this by having no nullable columns anywhere in their schemas. A browser over *your* arbitrary schema has no such luxury, so on JS and WasmJS the panel explains the limitation rather than failing at runtime. Android, desktop and iOS are unaffected.

## Release builds

Swap `database-inspector-ui` / `database-inspector-room` for `database-inspector-noop` — see [Release Builds](../release-builds.md). In the noop variant `attach()` accepts your database and ignores it; no connection is opened and no schema is read.
