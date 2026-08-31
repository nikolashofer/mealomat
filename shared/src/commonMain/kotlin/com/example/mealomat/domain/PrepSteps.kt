package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Prep_step_override
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.LocalDate

data class PrepStep(
    val key: String,
    val label: String,
    val amount: Double,
    val items: List<PrepStepItem>,
    val doneAt: Long?,
)

data class PrepStepItem(
    val date: LocalDate,
    val planItemId: String,
    val ingredientId: String,
    val amount: Double,
    val preppedAt: Long?,
)

// Builds a sessions steps: the windows PREP items, grouped by key, in the blocks saved order.
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
                items = covered.map { (date, _, item) ->
                    PrepStepItem(date, item.planItemId, item.ingredientId, item.amount, item.preppedAt)
                },
                doneAt = covered.mapNotNull { (_, _, item) -> item.preppedAt }
                    .takeIf { it.size == covered.size }?.max(),
            )
        }
        .sortedWith(compareBy({ positions[it.key] ?: Long.MAX_VALUE }, { it.label }))
}

private fun stepKey(item: DayItemView, components: List<Plan_component>): String {
    val lineage = components.firstOrNull { it.id == item.componentId }?.lineage_id
    return if (lineage != null) "component:$lineage" else "ingredient:${item.ingredientId}"
}
