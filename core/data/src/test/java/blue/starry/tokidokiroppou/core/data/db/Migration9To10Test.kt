package blue.starry.tokidokiroppou.core.data.db

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteTransactionListener
import android.os.CancellationSignal
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import blue.starry.tokidokiroppou.core.data.di.DataProvidesModule
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migration9To10Test {
    @Test
    fun migration9To10MigratesLegacyLawCodeNamesAndCreatesLawsTable() {
        val database = FakeMigrationDatabase().apply {
            createVersion9Schema()
            insertAllLegacyLawCodes("CIVIL_CODE")
            insertAllLegacyLawCodes("UNKNOWN_LAW")
        }

        DataProvidesModule.MIGRATION_9_10.migrate(database)

        assertEquals("129AC0000000089", database.lawCodes("articles").first())
        assertEquals("129AC0000000089", database.lawCodes("bookmarks").first())
        assertEquals("129AC0000000089", database.lawCodes("law_metadata").first())
        assertEquals("129AC0000000089", database.lawCodes("structure_headings").first())
        assertEquals("UNKNOWN_LAW", database.lawCodes("articles").last())
        assertEquals("UNKNOWN_LAW", database.lawCodes("bookmarks").last())
        assertEquals("UNKNOWN_LAW", database.lawCodes("law_metadata").last())
        assertEquals("UNKNOWN_LAW", database.lawCodes("structure_headings").last())
        assertTrue(database.hasTable("laws"))
    }

    private class FakeMigrationDatabase : SupportSQLiteDatabase {
        private val tables = mutableMapOf<String, MutableList<MutableMap<String, String>>>()

        override var version: Int = 9
        override val maximumSize: Long = Long.MAX_VALUE
        override var pageSize: Long = 4096
        override val isReadOnly: Boolean = false
        override val isOpen: Boolean = true
        override val path: String? = null
        override val isDbLockedByCurrentThread: Boolean = false
        override val isWriteAheadLoggingEnabled: Boolean = false
        override val attachedDbs: List<Pair<String, String>>? = emptyList()
        override val isDatabaseIntegrityOk: Boolean = true

        fun createVersion9Schema() {
            listOf("articles", "bookmarks", "law_metadata", "structure_headings").forEach { table ->
                tables[table] = mutableListOf()
            }
        }

        fun insertAllLegacyLawCodes(lawCode: String) {
            tables.getValue("articles").add(mutableMapOf("lawCode" to lawCode))
            tables.getValue("bookmarks").add(mutableMapOf("lawCode" to lawCode))
            tables.getValue("law_metadata").add(mutableMapOf("lawCode" to lawCode))
            tables.getValue("structure_headings").add(mutableMapOf("lawCode" to lawCode))
        }

        fun lawCodes(tableName: String): List<String> {
            return tables.getValue(tableName).map { it.getValue("lawCode") }
        }

        fun hasTable(tableName: String): Boolean {
            return tables.containsKey(tableName)
        }

        override fun execSQL(sql: String) {
            val normalizedSql = sql.trim().replace(Regex("\\s+"), " ")
            when {
                normalizedSql.startsWith("CREATE TABLE IF NOT EXISTS laws", ignoreCase = true) -> {
                    assertEquals(
                        "CREATE TABLE IF NOT EXISTS laws ( lawId TEXT NOT NULL, displayName TEXT NOT NULL, lawNum TEXT, category TEXT NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(lawId) )",
                        normalizedSql,
                    )
                    tables["laws"] = mutableListOf()
                }

                normalizedSql.startsWith("UPDATE ", ignoreCase = true) -> {
                    updateLawCode(normalizedSql)
                }

                else -> error("Unexpected SQL: $sql")
            }
        }

        override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
            execSQL(sql)
        }

        private fun updateLawCode(sql: String) {
            val match = requireNotNull(
                Regex(
                    pattern = "UPDATE (\\w+) SET lawCode = '([^']+)' WHERE lawCode = '([^']+)'",
                    option = RegexOption.IGNORE_CASE,
                )
                    .matchEntire(sql),
            ) {
                "Unexpected update SQL: $sql"
            }
            val tableName = match.groupValues[1]
            val newLawCode = match.groupValues[2]
            val oldLawCode = match.groupValues[3]
            tables.getValue(tableName).forEach { row ->
                if (row["lawCode"] == oldLawCode) {
                    row["lawCode"] = newLawCode
                }
            }
        }

        override fun compileStatement(sql: String): SupportSQLiteStatement {
            unsupported()
        }

        override fun beginTransaction() = Unit
        override fun beginTransactionNonExclusive() = Unit
        override fun beginTransactionWithListener(transactionListener: SQLiteTransactionListener) = Unit
        override fun beginTransactionWithListenerNonExclusive(transactionListener: SQLiteTransactionListener) = Unit
        override fun endTransaction() = Unit
        override fun setTransactionSuccessful() = Unit
        override fun inTransaction(): Boolean = false
        override fun yieldIfContendedSafely(): Boolean = false
        override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = false
        override fun setMaximumSize(numBytes: Long): Long = numBytes
        override fun query(query: String): Cursor = unsupported()
        override fun query(query: String, bindArgs: Array<out Any?>): Cursor = unsupported()
        override fun query(query: SupportSQLiteQuery): Cursor = unsupported()
        override fun query(query: SupportSQLiteQuery, cancellationSignal: CancellationSignal?): Cursor = unsupported()
        override fun insert(table: String, conflictAlgorithm: Int, values: ContentValues): Long = unsupported()
        override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = unsupported()
        override fun update(
            table: String,
            conflictAlgorithm: Int,
            values: ContentValues,
            whereClause: String?,
            whereArgs: Array<out Any?>?,
        ): Int = unsupported()

        override fun needUpgrade(newVersion: Int): Boolean = newVersion > version
        override fun setLocale(locale: Locale) = Unit
        override fun setMaxSqlCacheSize(cacheSize: Int) = Unit
        override fun setForeignKeyConstraintsEnabled(enabled: Boolean) = Unit
        override fun enableWriteAheadLogging(): Boolean = true
        override fun disableWriteAheadLogging() = Unit
        override fun close() = Unit

        private fun unsupported(): Nothing {
            throw SQLException("Unsupported fake database operation.")
        }
    }
}
