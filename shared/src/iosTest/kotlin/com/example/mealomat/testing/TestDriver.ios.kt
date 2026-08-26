package com.example.mealomat.testing

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.example.mealomat.data.db.MealomatDatabase

actual fun testDriver(): SqlDriver = inMemoryDriver(MealomatDatabase.Schema)
