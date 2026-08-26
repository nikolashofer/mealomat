package com.example.mealomat.testing

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.mealomat.data.db.MealomatDatabase

actual fun testDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { MealomatDatabase.Schema.create(it) }
