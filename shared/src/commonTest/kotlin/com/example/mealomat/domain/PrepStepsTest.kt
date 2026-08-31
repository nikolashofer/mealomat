package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Prep_step_override
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PrepStepsTest {

    private val monday = LocalDate(2026, 6, 22)
    private val tuesday = LocalDate(2026, 6, 23)
    private val wednesday = LocalDate(2026, 6, 24)
    private val window = Window(Slot(monday, 0), Slot(LocalDate(2026, 6, 25), 0))

    private fun item(
        ingredientId: String,
        amount: Double,
        mode: PrepMode = PrepMode.PREP,
        componentId: String? = null,
        excluded: Boolean = false,
        preppedAt: Long? = null,
    ) = DayItemView(
        planItemId = "$ingredientId-$amount-$componentId",
        ingredientId = ingredientId,
        amount = amount,
        position = 0,
        prepMode = mode,
        componentId = componentId,
        excluded = excluded,
        preppedAt = preppedAt,
    )

    private fun component(lineage: String) = Plan_component(
        id = "c-$lineage", user_id = "user-1", plan_id = "p", plan_meal_id = "m", lineage_id = lineage,
        updated_at = 0, deleted_at = null, name = "Peanut sauce", position = 0, prep_mode = PrepMode.PREP,
    )

    private fun day(date: LocalDate, items: List<DayItemView>, components: List<Plan_component> = emptyList()) =
        Day(date, listOf(DayMealView("meal-$date", "Lunch", 0, components, items)))

    private fun steps(days: List<Day>, order: List<Prep_step_override> = emptyList()) =
        prepStepsIn(window, days, order) { it.substringAfter(":") }

    @Test
    fun theSameIngredientAcrossDaysIsOneStep() {
        val result = steps(
            listOf(
                day(monday, listOf(item("rice", 300.0))),
                day(tuesday, listOf(item("rice", 300.0))),
                day(wednesday, listOf(item("rice", 300.0))),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(900.0, result.single().amount, "combined across the meals it serves")
        assertEquals(3, result.single().items.size)
    }

    @Test
    fun aFreshLineIsNoStep() {
        val result = steps(listOf(day(monday, listOf(item("salad", 80.0, mode = PrepMode.FRESH)))))

        assertEquals(emptyList(), result)
    }

    @Test
    fun anItemUnderAComponentKeysOnItsLineage() {
        val result = steps(
            listOf(day(monday, listOf(item("peanut", 40.0, componentId = "c-sauce")), listOf(component("sauce")))),
        )

        assertEquals("component:sauce", result.single().key, "so a saved ordering survives a revision")
    }

    @Test
    fun anExcludedLineDropsOutAndCanEmptyAStep() {
        val partly = steps(
            listOf(
                day(monday, listOf(item("rice", 300.0))),
                day(tuesday, listOf(item("rice", 300.0, excluded = true))),
            ),
        )
        assertEquals(300.0, partly.single().amount, "the eat-out is not prepped")

        val entirely = steps(listOf(day(monday, listOf(item("rice", 300.0, excluded = true)))))
        assertEquals(emptyList(), entirely)
    }

    @Test
    fun orderFollowsTheBlocksSavedOrdering() {
        val order = listOf(
            Prep_step_override("o1", "user-1", "block-1", 0, null, "ingredient:rice", 0),
            Prep_step_override("o2", "user-1", "block-1", 0, null, "ingredient:beans", 1),
        )
        val result = steps(
            listOf(day(monday, listOf(item("beans", 100.0), item("rice", 300.0), item("zucchini", 50.0)))),
            order,
        )

        assertEquals(listOf("rice", "beans", "zucchini"), result.map { it.label }, "unordered steps last")
    }

    @Test
    fun aStepIsDoneOnlyWhenEveryLineIs() {
        val partial = steps(
            listOf(
                day(monday, listOf(item("rice", 300.0, preppedAt = 100))),
                day(tuesday, listOf(item("rice", 300.0))),
            ),
        )
        assertNull(partial.single().doneAt, "one line still to make")

        val done = steps(
            listOf(
                day(monday, listOf(item("rice", 300.0, preppedAt = 100))),
                day(tuesday, listOf(item("rice", 300.0, preppedAt = 200))),
            ),
        )
        assertEquals(200L, assertNotNull(done.single().doneAt), "when the last line was made")
    }
}
