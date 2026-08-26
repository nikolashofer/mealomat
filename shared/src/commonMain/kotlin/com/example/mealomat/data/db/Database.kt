package com.example.mealomat.data.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver

val basisAdapter = object : ColumnAdapter<Basis, String> {
    override fun decode(databaseValue: String) = Basis.valueOf(databaseValue)
    override fun encode(value: Basis) = value.name
}

fun mealomatDatabase(driver: SqlDriver) = MealomatDatabase(
    driver = driver,
    ingredientAdapter = Ingredient.Adapter(basisAdapter = basisAdapter),
)
