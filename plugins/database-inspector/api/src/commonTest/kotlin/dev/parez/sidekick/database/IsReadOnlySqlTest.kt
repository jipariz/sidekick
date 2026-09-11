package dev.parez.sidekick.database

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This guard is the only thing standing between a mistyped statement and a developer's real
 * database, so the rejection cases matter more than the acceptance ones.
 */
class IsReadOnlySqlTest {

    @Test
    fun `accepts the three read-only statement kinds in any case`() {
        assertTrue(isReadOnlySql("SELECT * FROM users"))
        assertTrue(isReadOnlySql("select * from users"))
        assertTrue(isReadOnlySql("SeLeCt 1"))
        assertTrue(isReadOnlySql("PRAGMA table_info(users)"))
        assertTrue(isReadOnlySql("pragma table_info(users)"))
        assertTrue(isReadOnlySql("EXPLAIN SELECT 1"))
    }

    @Test
    fun `accepts leading and trailing whitespace and a trailing semicolon`() {
        assertTrue(isReadOnlySql("   SELECT 1   "))
        assertTrue(isReadOnlySql("SELECT 1;"))
        assertTrue(isReadOnlySql("  SELECT 1 ;  "))
    }

    @Test
    fun `accepts a parenthesised select`() {
        assertTrue(isReadOnlySql("SELECT(1)"))
    }

    @Test
    fun `rejects every mutating statement`() {
        listOf(
                "DELETE FROM users",
                "DROP TABLE users",
                "UPDATE users SET name = 'x'",
                "INSERT INTO users VALUES (1)",
                "ALTER TABLE users ADD COLUMN x TEXT",
                "CREATE TABLE t (id INTEGER)",
                "REPLACE INTO users VALUES (1)",
                "TRUNCATE TABLE users",
                "VACUUM",
                "ATTACH DATABASE 'other.db' AS other",
            )
            .forEach { assertFalse(isReadOnlySql(it), "should reject: $it") }
    }

    @Test
    fun `rejects a CTE because SQLite lets one precede a delete`() {
        // The reason `WITH` is deliberately absent from the allowlist: this is valid
        // SQLite and would sail past a check that only looked for a mutating keyword.
        assertFalse(isReadOnlySql("WITH doomed AS (SELECT id FROM users) DELETE FROM users"))
        assertFalse(isReadOnlySql("WITH x AS (SELECT 1) SELECT * FROM x"))
    }

    @Test
    fun `rejects a second statement smuggled in after a semicolon`() {
        assertFalse(isReadOnlySql("SELECT 1; DROP TABLE users"))
        assertFalse(isReadOnlySql("SELECT 1; DROP TABLE users;"))
        assertFalse(isReadOnlySql("PRAGMA foo; DELETE FROM users"))
    }

    @Test
    fun `rejects empty and blank input`() {
        assertFalse(isReadOnlySql(""))
        assertFalse(isReadOnlySql("   "))
        assertFalse(isReadOnlySql(";"))
        assertFalse(isReadOnlySql("  ;  "))
    }

    @Test
    fun `rejects a keyword that merely starts with an allowed one`() {
        assertFalse(isReadOnlySql("SELECTOR FROM x"))
        assertFalse(isReadOnlySql("DROPSELECT"))
    }
}
