package com.example.mealomat.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.auth.SessionScopedData

// wipes the local db on sign-out by dropping every user table and recreating the schema
class DatabaseSessionScopedData(private val driver: SqlDriver) : SessionScopedData {

    override suspend fun clear() {
        userTables().forEach { driver.execute(null, "DROP TABLE IF EXISTS \"$it\"", 0).await() }
        MealomatDatabase.Schema.create(driver).await()
    }

    private suspend fun userTables(): List<String> = driver.executeQuery(
        identifier = null,
        sql = """
            SELECT name FROM sqlite_master
            WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'
        """.trimIndent(),
        mapper = { cursor ->
            QueryResult.Value(
                buildList { while (cursor.next().value) add(cursor.getString(0)!!) },
            )
        },
        parameters = 0,
    ).await()
}
