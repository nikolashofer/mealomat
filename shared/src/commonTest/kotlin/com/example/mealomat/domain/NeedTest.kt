package com.example.mealomat.domain

import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NeedTest {

    private val thursday = LocalDate(2026, 6, 25)
    private val friday = LocalDate(2026, 6, 26)
    private val sunday = LocalDate(2026, 6, 28)

    private fun item(ingredientId: String, amount: Double, position: Int, excluded: Boolean = false) =
        DayItemView(
            planItemId = "$ingredientId-$position",
            ingredientId = ingredientId,
            amount = amount,
            position = position,
            prepMode = PrepMode.FRESH,
            excluded = excluded,
        )

    private fun day(date: LocalDate, vararg meals: DayMealView) = Day(date, meals.toList())

    private fun meal(position: Int, vararg items: DayItemView) =
        DayMealView("meal-$position", "Meal $position", position, emptyList(), items.toList())

    private val window = Window(Slot(thursday, 1), Slot(sunday, 1))

    @Test
    fun theEndDaysArePartial() {
        val days = listOf(
            day(thursday, meal(0, item("rice", 100.0, 0)), meal(1, item("rice", 200.0, 0))),
            day(friday, meal(0, item("rice", 400.0, 0))),
            day(sunday, meal(0, item("rice", 800.0, 0)), meal(1, item("rice", 1600.0, 0))),
        )

        val total = usesIn(window, days).sumOf { it.amount }

        assertEquals(1400.0, total, "Thursday's first meal and Sunday's second one are outside")
    }

    @Test
    fun anExcludedLineIsNotBought() {
        val days = listOf(day(friday, meal(0, item("rice", 100.0, 0), item("egg", 2.0, 1, excluded = true))))

        val needs = needsFrom(usesIn(window, days), have = { 0.0 }, packSize = { null })

        assertEquals(listOf("rice"), needs.map { it.ingredientId })
    }

    @Test
    fun buyIsWhatIsMissing() {
        val days = listOf(day(friday, meal(0, item("rice", 500.0, 0))))

        val needs = needsFrom(usesIn(window, days), have = { 200.0 }, packSize = { null })

        assertEquals(Need("rice", need = 500.0, have = 200.0, buy = 300.0), needs.single())
    }

    @Test
    fun ampleStockBuysNothingRatherThanANegativeAmount() {
        val days = listOf(day(friday, meal(0, item("rice", 500.0, 0))))

        val needs = needsFrom(usesIn(window, days), have = { 900.0 }, packSize = { null })

        assertEquals(0.0, needs.single().buy)
        assertEquals(900.0, needs.single().have, "have is still reported as it is")
    }

    @Test
    fun packRoundingGoesUpToWholePacks() {
        assertEquals(500.0, packRound(340.0, packSize = 500.0))
        assertEquals(1000.0, packRound(501.0, packSize = 500.0))
        assertEquals(500.0, packRound(500.0, packSize = 500.0), "an exact fit is one pack")
    }

    @Test
    fun anIngredientWithNoPackSizeIsAskedForExactly() {
        assertEquals(340.0, packRound(340.0, packSize = null))
        assertEquals(0.0, packRound(0.0, packSize = 500.0), "nothing to buy stays nothing")
    }
}
