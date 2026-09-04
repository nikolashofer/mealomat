package com.example.mealomat.feature.prep.model

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.domain.MeasureLabel
import com.example.mealomat.domain.PrepStep
import com.example.mealomat.domain.amountLabel
import kotlinx.datetime.LocalDate

enum class PrepLineState { Done, Pending }

data class PrepIngredient(val name: String, val amount: MeasureLabel)

data class PrepPortion(
    val date: LocalDate,
    val weekday: String,
    val amount: MeasureLabel?,
    val share: Float,
)

data class PrepLine(
    val key: String,
    val name: String,
    val state: PrepLineState,
    val make: MeasureLabel,
    val ingredients: List<PrepIngredient>,
    val portions: List<PrepPortion>,
)

fun prepLines(steps: List<PrepStep>, ingredients: Map<String, Ingredient>): List<PrepLine> =
    steps.map { step ->
        val lines = step.totals.mapNotNull { total ->
            ingredients[total.ingredientId]?.let { PrepIngredient(it.name, amountLabel(total.amount, it.basis)) }
        }
        val basis = step.items.mapNotNull { ingredients[it.ingredientId]?.basis }.distinct().singleOrNull()
        val portions = portionsOf(step, basis)

        PrepLine(
            key = step.key,
            name = step.label,
            state = if (step.doneAt != null) PrepLineState.Done else PrepLineState.Pending,
            make = makeLabel(step, basis, portions.size),
            ingredients = lines,
            portions = portions,
        )
    }

private fun makeLabel(step: PrepStep, basis: Basis?, boxes: Int): MeasureLabel = when {
    step.totals.size == 1 && basis != null -> amountLabel(step.totals.single().amount, basis)
    else -> MeasureLabel(boxes.toString(), if (boxes == 1) "box" else "boxes")
}

private fun portionsOf(step: PrepStep, basis: Basis?): List<PrepPortion> {
    val byDate = step.items.groupBy { it.date }
    val amounts = byDate.mapValues { (_, items) -> items.sumOf { it.amount } }
    val largest = amounts.values.maxOrNull() ?: 0.0

    return byDate.keys.sorted().map { date ->
        val amount = amounts.getValue(date)
        PrepPortion(
            date = date,
            weekday = date.dayOfWeek.name.take(3),
            amount = basis?.let { amountLabel(amount, it) },
            share = if (basis == null || largest <= 0.0) 1f else (amount / largest).toFloat(),
        )
    }
}
