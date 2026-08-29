package com.example.mealomat.domain

import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Plan_item
import com.example.mealomat.data.db.Plan_meal
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class PlanVersion(
    val meals: List<Plan_meal>,
    val components: List<Plan_component>,
    val items: List<Plan_item>,
)

data class DayItemView(
    val planItemId: String,
    val ingredientId: String,
    val amount: Double,
    val position: Int,
    val prepMode: PrepMode,
    val componentId: String? = null,
    val excluded: Boolean = false,
    val preppedAt: Long? = null,
    val tickedAt: Long? = null,
) {
    val isReady get() = preppedAt != null
    val isDone get() = preppedAt != null || tickedAt != null || excluded
}

data class DayMealView(
    val planMealId: String,
    val name: String,
    val position: Int,
    val components: List<Plan_component>,
    val items: List<DayItemView>,
) {
    val isDone get() = items.isNotEmpty() && items.all { it.isDone }
}

data class Day(val date: LocalDate, val meals: List<DayMealView>)

fun Day.ingredientUses(): List<IngredientUse> = meals
    .flatMap { it.items }
    .map { IngredientUse(it.ingredientId, it.amount, it.excluded) }

// TODO: not tested for now it is pretty trivial, maybe add tests later
// Builds a day: the version's meals for that weekday, each item carrying whatever state was recorded
// against it. Amounts are read from the version, never copied, so history cannot drift.
fun projectDay(
    date: LocalDate,
    weekday: DayOfWeek,
    plan: PlanVersion,
    state: List<Day_item>,
): Day {
    val stateByItem = state.associateBy { it.plan_item_id }
    val componentsByMeal = plan.components.groupBy { it.plan_meal_id }
    val itemsByMeal = plan.items.groupBy { it.plan_meal_id }

    val meals = plan.meals
        .filter { it.weekday == weekday }
        .sortedBy { it.position }
        .map { meal ->
            val components = componentsByMeal[meal.id].orEmpty().sortedBy { it.position }
            val byId = components.associateBy { it.id }
            DayMealView(
                planMealId = meal.id,
                name = meal.name,
                position = meal.position.toInt(),
                components = components,
                items = itemsByMeal[meal.id].orEmpty().sortedBy { it.position }.map { item ->
                    val recorded = stateByItem[item.id]
                    DayItemView(
                        planItemId = item.id,
                        ingredientId = item.ingredient_id,
                        amount = item.amount,
                        position = item.position.toInt(),
                        prepMode = resolvePrepMode(item.prep_mode, byId[item.plan_component_id]?.prep_mode),
                        componentId = item.plan_component_id,
                        excluded = recorded?.excluded ?: false,
                        preppedAt = recorded?.prepped_at,
                        tickedAt = recorded?.ticked_at,
                    )
                },
            )
        }
    return Day(date, meals)
}

// NULL on a plan item means inherit from its component; with no component it is FRESH.
fun resolvePrepMode(itemPrepMode: PrepMode?, componentPrepMode: PrepMode?): PrepMode =
    itemPrepMode ?: componentPrepMode ?: PrepMode.FRESH
