package com.example.mealomat.feature.shopping.model

import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.MeasureLabel
import com.example.mealomat.domain.amountLabel
import kotlin.math.ceil

enum class LineState { Bought, Skipped, Pending }

data class ShoppingLine(
    val ingredientId: String,
    val name: String,
    val state: LineState,
    val need: MeasureLabel,
    val have: MeasureLabel,
    val buy: MeasureLabel,
    val buyAmount: Double,
    val short: MeasureLabel,
    val bought: MeasureLabel?,
)

fun suggestedBuy(need: IngredientNeed): Double = when (val pack = need.packSize) {
    null -> need.buy
    else -> ceil(need.buy / pack) * pack
}

fun shoppingLines(
    needs: List<IngredientNeed>,
    steps: List<Shopping_step>,
    ingredients: Map<String, Ingredient>,
): List<ShoppingLine> {
    val byIngredient = needs.associateBy { it.ingredientId }
    val acted = steps.mapNotNull { step ->
        val ingredient = ingredients[step.ingredient_id] ?: return@mapNotNull null
        step.toLine(ingredient, byIngredient[step.ingredient_id])
    }
    val settled = steps.map { it.ingredient_id }.toSet()
    val pending = needs
        .filter { it.buy > 0.0 && it.ingredientId !in settled }
        .mapNotNull { need ->
            ingredients[need.ingredientId]?.let { need.toLine(it, LineState.Pending, bought = null) }
        }

    return (acted + pending).sortedBy { it.ingredientId }
}

private fun Shopping_step.toLine(ingredient: Ingredient, need: IngredientNeed?): ShoppingLine {
    val snapshot = IngredientNeed(
        ingredientId = ingredient_id,
        need = needed_amount,
        have = have_amount,
        buy = suggested_amount,
        packSize = ingredient.pack_size,
    )
    return (need ?: snapshot).toLine(
        ingredient = ingredient,
        state = if (bought_amount != null) LineState.Bought else LineState.Skipped,
        bought = bought_amount?.let { amountLabel(it, ingredient.basis) },
    )
}

private fun IngredientNeed.toLine(
    ingredient: Ingredient,
    state: LineState,
    bought: MeasureLabel?,
): ShoppingLine {
    val amount = suggestedBuy(this)
    return ShoppingLine(
        ingredientId = ingredientId,
        name = ingredient.name,
        state = state,
        need = amountLabel(need, ingredient.basis),
        have = amountLabel(have, ingredient.basis),
        buy = amountLabel(amount, ingredient.basis),
        buyAmount = amount,
        short = amountLabel(buy, ingredient.basis),
        bought = bought,
    )
}
