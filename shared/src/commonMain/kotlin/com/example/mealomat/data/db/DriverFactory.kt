package com.example.mealomat.data.db

import app.cash.sqldelight.db.SqlDriver

const val DATABASE_NAME = "mealomat.db"

expect class DriverFactory {
    fun create(): SqlDriver
}
