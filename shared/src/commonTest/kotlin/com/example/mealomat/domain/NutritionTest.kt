package com.example.mealomat.domain

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionTest {

    private fun ingredient(id: String, basis: Basis, kcal: Double, protein: Double = 0.0) = Ingredient(
        id = id, user_id = "user-1", updated_at = 0, deleted_at = null,
        name = id, basis = basis, kcal = kcal,
        protein_g = protein, carbs_g = 0.0, fat_g = 0.0,
        fiber_g = null, sugar_g = null, saturated_fat_g = null, salt_g = null,
        pack_size = null, archived = false, note = null,
    )

    @Test
    fun perHundredBasesScaleByAmountOverHundred() {
        val oats = ingredient("oats", Basis.G100, kcal = 350.0)
        assertEquals(280.0, macrosOf(oats, 80.0).kcal)

        val milk = ingredient("milk", Basis.ML100, kcal = 50.0)
        assertEquals(125.0, macrosOf(milk, 250.0).kcal)
    }

    @Test
    fun unitBasisIsPerOne() {
        val egg = ingredient("egg", Basis.UNIT, kcal = 77.0)
        assertEquals(154.0, macrosOf(egg, 2.0).kcal)
    }

    @Test
    fun totalsSumAcrossLines() {
        val ingredients = mapOf(
            "oats" to ingredient("oats", Basis.G100, kcal = 350.0, protein = 13.0),
            "egg" to ingredient("egg", Basis.UNIT, kcal = 77.0, protein = 7.0),
        )
        val total = totalOf(
            listOf(IngredientUse("oats", 100.0), IngredientUse("egg", 2.0)),
            ingredients,
        )

        assertEquals(504.0, total.kcal)
        assertEquals(27.0, total.proteinG)
    }

    @Test
    fun excludedLinesContributeNothing() {
        val ingredients = mapOf("oats" to ingredient("oats", Basis.G100, kcal = 350.0))
        val total = totalOf(
            listOf(IngredientUse("oats", 100.0), IngredientUse("oats", 100.0, excluded = true)),
            ingredients,
        )

        assertEquals(350.0, total.kcal)
    }

    @Test
    fun anUnknownIngredientIsSkippedNotFatal() {
        val ingredients = mapOf("oats" to ingredient("oats", Basis.G100, kcal = 350.0))
        val total = totalOf(
            listOf(IngredientUse("oats", 100.0), IngredientUse("ghost", 999.0)),
            ingredients,
        )

        assertEquals(350.0, total.kcal)
    }

    private val oats = ingredient("oats", Basis.G100, kcal = 370.0)

    private val thursday = LocalDate(2026, 6, 25)

    private fun line(
        ingredientId: String,
        amount: Double,
        ticked: Long? = null,
        prepped: Long? = null,
        excluded: Boolean = false,
    ) = DayItemView(
        planItemId = "$ingredientId-$amount",
        ingredientId = ingredientId,
        amount = amount,
        position = 0,
        prepMode = PrepMode.FRESH,
        excluded = excluded,
        preppedAt = prepped,
        tickedAt = ticked,
    )

    private fun dayOf(vararg items: DayItemView) =
        Day(thursday, listOf(DayMealView("meal", "Lunch", 0, emptyList(), items.toList())))

    @Test
    fun eatenCountsTickedLinesAndPlannedCountsAll() {
        val totals = totalsOf(
            dayOf(line("oats", 100.0, ticked = 1), line("oats", 100.0)),
            mapOf("oats" to oats),
        )

        assertEquals(740.0, totals.planned.kcal)
        assertEquals(370.0, totals.eaten.kcal, "only what was ticked off")
    }

    @Test
    fun preppedIsReadyRatherThanEaten() {
        val totals = totalsOf(dayOf(line("oats", 100.0, prepped = 1)), mapOf("oats" to oats))

        assertEquals(370.0, totals.planned.kcal)
        assertEquals(0.0, totals.eaten.kcal, "prepped food has left the pantry, not been eaten")
    }

    @Test
    fun anExcludedLineIsNeitherPlannedNorEaten() {
        val totals = totalsOf(
            dayOf(line("oats", 100.0, ticked = 1, excluded = true)),
            mapOf("oats" to oats),
        )

        assertEquals(0.0, totals.planned.kcal)
        assertEquals(0.0, totals.eaten.kcal)
    }

    @Test
    fun gramsIsTheThreeMacrosTogether() {
        assertEquals(35.0, Macros(kcal = 500.0, proteinG = 10.0, carbsG = 20.0, fatG = 5.0).grams)
    }
}
