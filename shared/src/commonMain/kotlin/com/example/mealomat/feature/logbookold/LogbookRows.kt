package com.example.mealomat.feature.logbookold

import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.domain.Day
import com.example.mealomat.domain.DayItemView
import com.example.mealomat.domain.amountLabel
import com.example.mealomat.domain.totalOf
import com.example.mealomat.domain.use

data class MealRow(
    val id: String,
    val name: String,
    val kcal: Double,
    val items: List<ItemRow>,
)

data class ItemRow(
    val planItemId: String,
    val name: String,
    val amount: String,
    val prepped: Boolean,
    val ticked: Boolean,
    val excluded: Boolean,
)

fun logbookRows(day: Day, ingredients: Map<String, Ingredient>): List<MealRow> = day.meals.map { meal ->
    MealRow(
        id = meal.planMealId,
        name = meal.name,
        kcal = totalOf(meal.items.map { it.use() }, ingredients).kcal,
        items = meal.items.mapNotNull { item ->
            ingredients[item.ingredientId]?.let { item.toRow(it) }
        },
    )
}

private fun DayItemView.toRow(ingredient: Ingredient) = ItemRow(
    planItemId = planItemId,
    name = ingredient.name,
    amount = amountLabel(amount, ingredient.basis).text,
    prepped = preppedAt != null,
    ticked = tickedAt != null,
    excluded = excluded,
)
