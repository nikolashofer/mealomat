package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Prep_step_override
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.LocalDate

data class PrepStep(
    val key: String,        // "component:<lineage_id>" | "ingredient:<ingredient_id>"
    val label: String,
    val amount: Double,
    val lines: List<PrepLine>,
    val doneAt: Long?,      // set once every covered line is prepped; null while partial
)

data class PrepLine(
    val date: LocalDate,
    val planItemId: String,
    val ingredientId: String,
    val amount: Double,
    val preppedAt: Long?,
)

// The steps a session walks: the window's PREP lines, grouped by key, in the block's saved order.
fun prepStepsIn(
    window: Window,
    days: List<Day>,
    order: List<Prep_step_override>,
    labels: (String) -> String,
): List<PrepStep> {
    val positions = order.associate { it.target_key to it.position }
    return days
        .flatMap { day -> day.meals.filter { Slot(day.date, it.position) in window }.map { day.date to it } }
        .flatMap { (date, meal) -> meal.items.map { Triple(date, meal.components, it) } }
        .filter { (_, _, item) -> item.prepMode == PrepMode.PREP && !item.excluded }
        .groupBy { (_, components, item) -> stepKey(item, components) }
        .map { (key, covered) ->
            PrepStep(
                key = key,
                label = labels(key),
                amount = covered.sumOf { (_, _, item) -> item.amount },
                lines = covered.map { (date, _, item) ->
                    PrepLine(date, item.planItemId, item.ingredientId, item.amount, item.preppedAt)
                },
                doneAt = covered.mapNotNull { (_, _, item) -> item.preppedAt }
                    .takeIf { it.size == covered.size }?.max(),
            )
        }
        .sortedWith(compareBy({ positions[it.key] ?: Long.MAX_VALUE }, { it.label }))
}

// A components lineage rather than its row, so a saved ordering survives a plan revision.
private fun stepKey(item: DayItemView, components: List<Plan_component>): String {
    val lineage = components.firstOrNull { it.id == item.componentId }?.lineage_id
    return if (lineage != null) "component:$lineage" else "ingredient:${item.ingredientId}"
}
