package com.example.mealomat.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(MealomatDatabase.Schema, DATABASE_NAME)
}
