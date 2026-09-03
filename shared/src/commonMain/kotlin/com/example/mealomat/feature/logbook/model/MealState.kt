package com.example.mealomat.feature.logbook.model


fun MealRow.isComplete(): Boolean = items.isNotEmpty() && items.all { it.isSettled() }

fun nextMeal(meals: List<MealRow>): MealRow? = meals.firstOrNull { meal -> !meal.isComplete() }

private fun ItemRow.isSettled() = ticked || excluded
