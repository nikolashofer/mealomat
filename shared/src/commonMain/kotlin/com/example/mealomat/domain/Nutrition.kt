package com.example.mealomat.domain

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient

data class Macros(
    val kcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
) {
    operator fun plus(other: Macros) = Macros(
        kcal = kcal + other.kcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
    )
}

val Macros.grams: Double get() = proteinG + carbsG + fatG

data class DayTotals(val eaten: Macros, val planned: Macros)

data class IngredientUse(
    val ingredientId: String,
    val amount: Double,
    val excluded: Boolean = false,
)

// amount * macros-per-basis. G100/ML100 are per 100, UNIT is per one.
fun macrosOf(ingredient: Ingredient, amount: Double): Macros {
    val factor = when (ingredient.basis) {
        Basis.G100, Basis.ML100 -> amount / 100.0
        Basis.UNIT -> amount
    }
    return Macros(
        kcal = ingredient.kcal * factor,
        proteinG = ingredient.protein_g * factor,
        carbsG = ingredient.carbs_g * factor,
        fatG = ingredient.fat_g * factor,
    )
}

// one total from a set of uses: a component, a meal, a day. excluded uses contribute nothing.
fun totalOf(uses: List<IngredientUse>, ingredients: Map<String, Ingredient>): Macros =
    uses.filterNot { it.excluded }
        .fold(Macros()) { total, use ->
            val ingredient = ingredients[use.ingredientId] ?: return@fold total
            total + macrosOf(ingredient, use.amount)
        }

fun totalsOf(day: Day, ingredients: Map<String, Ingredient>): DayTotals = DayTotals(
    eaten = totalOf(day.ingredientUses { it.tickedAt != null }, ingredients),
    planned = totalOf(day.ingredientUses(), ingredients),
)
