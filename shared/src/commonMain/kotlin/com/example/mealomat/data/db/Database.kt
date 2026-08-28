package com.example.mealomat.data.db

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

val basisAdapter = object : ColumnAdapter<Basis, String> {
    override fun decode(databaseValue: String) = Basis.valueOf(databaseValue)
    override fun encode(value: Basis) = value.name
}

val prepModeAdapter = object : ColumnAdapter<PrepMode, String> {
    override fun decode(databaseValue: String) = PrepMode.valueOf(databaseValue)
    override fun encode(value: PrepMode) = value.name
}

// the ISO day number (1 = monday, ... 7 = sunday)
val dayOfWeekAdapter = object : ColumnAdapter<DayOfWeek, Long> {
    override fun decode(databaseValue: Long) = DayOfWeek(databaseValue.toInt())
    override fun encode(value: DayOfWeek) = value.isoDayNumber.toLong()
}

fun mealomatDatabase(driver: SqlDriver) = MealomatDatabase(
    driver = driver,
    ingredientAdapter = Ingredient.Adapter(basisAdapter = basisAdapter),
    plan_mealAdapter = Plan_meal.Adapter(weekdayAdapter = dayOfWeekAdapter),
    plan_componentAdapter = Plan_component.Adapter(prep_modeAdapter = prepModeAdapter),
    plan_itemAdapter = Plan_item.Adapter(prep_modeAdapter = prepModeAdapter),
    prep_blockAdapter = Prep_block.Adapter(
        prep_weekdayAdapter = dayOfWeekAdapter,
        shopping_weekdayAdapter = dayOfWeekAdapter,
        covers_from_weekdayAdapter = dayOfWeekAdapter,
    ),
)
