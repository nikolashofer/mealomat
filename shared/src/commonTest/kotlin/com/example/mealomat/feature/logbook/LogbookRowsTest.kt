package com.example.mealomat.feature.logbook

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.data.db.PrepMode
import com.example.mealomat.domain.Day
import com.example.mealomat.domain.DayItemView
import com.example.mealomat.domain.DayMealView
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogbookRowsTest {

    private fun ingredient(id: String, name: String, kcal: Double, basis: Basis = Basis.G100) = Ingredient(
        id = id, user_id = "user-1", updated_at = 0, deleted_at = null,
        name = name, basis = basis, kcal = kcal,
        protein_g = 0.0, carbs_g = 0.0, fat_g = 0.0,
        fiber_g = null, sugar_g = null, saturated_fat_g = null, salt_g = null,
        pack_size = null, archived = false, note = null,
    )

    private val library = mapOf(
        "rice" to ingredient("rice", "Rice", kcal = 350.0),
        "egg" to ingredient("egg", "Egg", kcal = 77.0, basis = Basis.UNIT),
    )

    private fun item(
        ingredientId: String,
        amount: Double,
        mode: PrepMode = PrepMode.FRESH,
        prepped: Long? = null,
        ticked: Long? = null,
        excluded: Boolean = false,
    ) = DayItemView(
        planItemId = "$ingredientId-$amount",
        ingredientId = ingredientId,
        amount = amount,
        position = 0,
        prepMode = mode,
        excluded = excluded,
        preppedAt = prepped,
        tickedAt = ticked,
    )

    private fun day(vararg items: DayItemView) = Day(
        LocalDate(2026, 6, 25),
        listOf(DayMealView("meal-1", "Lunch", 0, emptyList(), items.toList())),
    )

    @Test
    fun aMealsKcalIsItsItemsTogether() {
        val meal = logbookRows(day(item("rice", 100.0), item("egg", 2.0)), library).single()

        assertEquals("Lunch", meal.name)
        assertEquals(504.0, meal.kcal, "350 for the rice and 77 each for the eggs")
    }

    @Test
    fun anExcludedItemAddsNothingToTheTotal() {
        val meal = logbookRows(day(item("rice", 100.0), item("egg", 2.0, excluded = true)), library).single()

        assertEquals(350.0, meal.kcal)
        assertTrue(meal.items.single { it.name == "Egg" }.excluded, "but it is still listed")
    }

    @Test
    fun theAmountCarriesItsUnit() {
        val meal = logbookRows(day(item("rice", 180.0), item("egg", 2.0)), library).single()

        assertEquals(listOf("180 g", "2 units"), meal.items.map { it.amount })
    }

    @Test
    fun preppedMeansMadeRatherThanMeantTo() {
        val meal = logbookRows(
            day(
                item("rice", 100.0, mode = PrepMode.PREP, prepped = 1),
                item("egg", 2.0, mode = PrepMode.PREP),
            ),
            library,
        ).single()

        assertTrue(meal.items.first { it.name == "Rice" }.prepped)
        assertTrue(!meal.items.first { it.name == "Egg" }.prepped, "the session never reached it")
    }

    @Test
    fun theRowCarriesWhatHappenedToTheLine() {
        val meal = logbookRows(day(item("rice", 100.0, ticked = 5)), library).single()
        val row = meal.items.single()

        assertTrue(row.ticked)
        assertTrue(!row.prepped && !row.excluded)
        assertEquals("rice-100.0", row.planItemId)
    }

    @Test
    fun anItemWithNoIngredientIsSkipped() {
        val meal = logbookRows(day(item("rice", 100.0), item("ghost", 50.0)), library).single()

        assertEquals(listOf("Rice"), meal.items.map { it.name })
    }
}
