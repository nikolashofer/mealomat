package com.example.mealomat.domain

data class IngredientNeed(
    val ingredientId: String,
    val need: Double,
    val have: Double,
    val buy: Double,
    val packSize: Double? = null,
)

// Collects the planned amounts inside a window.
fun usesIn(window: Window, days: List<Day>): List<IngredientUse> = days.flatMap { day ->
    day.meals
        .filter { Slot(day.date, it.position) in window }
        .flatMap { meal -> meal.items.map { it.use() } }
}

// Sums uses per ingredient against pantry stock, dropping excluded ones.
fun needsFrom(
    uses: List<IngredientUse>,
    have: (String) -> Double,
    packSize: (String) -> Double?,
): List<IngredientNeed> = uses
    .filterNot { it.excluded }
    .groupBy { it.ingredientId }
    .map { (ingredientId, forIngredient) ->
        val need = forIngredient.sumOf { it.amount }
        val stock = have(ingredientId)
        IngredientNeed(
            ingredientId = ingredientId,
            need = need,
            have = stock,
            buy = maxOf(0.0, need - stock),
            packSize = packSize(ingredientId),
        )
    }
