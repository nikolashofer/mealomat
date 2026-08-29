package com.example.mealomat.domain

import kotlin.math.ceil

data class Need(
    val ingredientId: String,
    val need: Double,
    val have: Double,
    val buy: Double,
)

// the planned amounts inside a window. Meals are filtered by slot, since the end days are partial.
fun usesIn(window: Window, days: List<Day>): List<IngredientUse> = days.flatMap { day ->
    day.meals
        .filter { Slot(day.date, it.position) in window }
        .flatMap { meal -> meal.items.map { IngredientUse(it.ingredientId, it.amount, it.excluded) } }
}

// Uses summed per ingredient against what the pantry holds. Excluded
// uses contribute nothing, since a planned eat-out is not bought.
fun needsFrom(
    uses: List<IngredientUse>,
    have: (String) -> Double,
    packSize: (String) -> Double?,
): List<Need> = uses
    .filterNot { it.excluded }
    .groupBy { it.ingredientId }
    .map { (ingredientId, forIngredient) ->
        val need = forIngredient.sumOf { it.amount }
        val stock = have(ingredientId)
        Need(
            ingredientId = ingredientId,
            need = need,
            have = stock,
            buy = packRound(maxOf(0.0, need - stock), packSize(ingredientId)),
        )
    }

// Round up to whole packs, so the wizard asks for something a shop actually sells.
fun packRound(amount: Double, packSize: Double?): Double =
    if (packSize == null || packSize <= 0.0 || amount <= 0.0) amount
    else ceil(amount / packSize) * packSize
