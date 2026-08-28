package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan_meal
import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverageTest {

    private fun block(id: String, coversFrom: DayOfWeek, position: Long) = Prep_block(
        id = id,
        user_id = "user-1",
        updated_at = 0,
        deleted_at = null,
        name = id,
        prep_weekday = coversFrom,
        shopping_weekday = coversFrom,
        covers_from_weekday = coversFrom,
        covers_from_position = position,
    )

    private fun meal(weekday: DayOfWeek, position: Long, name: String) = Plan_meal(
        id = "${weekday.name.take(3)}-$name",
        user_id = "user-1",
        plan_id = "plan-1",
        updated_at = 0,
        deleted_at = null,
        weekday = weekday,
        name = name,
        position = position,
    )

    // breakfast, lunch, dinner every day of the week.
    private fun fullWeek() = DayOfWeek.entries.flatMap { day ->
        listOf(meal(day, 0, "Breakfast"), meal(day, 1, "Lunch"), meal(day, 2, "Dinner"))
    }

    private val midweek = block("midweek", DayOfWeek.THURSDAY, 0)
    private val weekend = block("weekend", DayOfWeek.SUNDAY, 1)

    @Test
    fun reproducesTheDefaultConfiguration() {
        val meals = fullWeek()
        val owners = coverageOf(listOf(midweek, weekend), meals)

        fun ownerOf(day: DayOfWeek, name: String) =
            owners.getValue(meals.single { it.weekday == day && it.name == name }.id)

        listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach { day ->
            assertEquals("midweek", ownerOf(day, "Dinner"), "$day should be midweek")
        }
        assertEquals("midweek", ownerOf(DayOfWeek.SUNDAY, "Breakfast"), "Sunday morning is midweek")
        assertEquals("weekend", ownerOf(DayOfWeek.SUNDAY, "Lunch"), "the rest of Sunday is weekend")
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY).forEach { day ->
            assertEquals("weekend", ownerOf(day, "Breakfast"), "$day should be weekend")
        }
    }

    @Test
    fun everyMealIsCoveredExactlyOnce() {
        val meals = fullWeek()
        val owners = coverageOf(listOf(midweek, weekend), meals)

        assertEquals(meals.size, owners.size)
        assertTrue(owners.values.all { it == "midweek" || it == "weekend" })
    }

    @Test
    fun aNewMealInsideAWindowIsCoveredAutomatically() {
        val meals = fullWeek() + meal(DayOfWeek.FRIDAY, 3, "Supper")
        val owners = coverageOf(listOf(midweek, weekend), meals)

        assertEquals("midweek", owners.getValue("FRI-Supper"))
    }

    @Test
    fun mealsBeforeTheEarliestBoundaryWrapToTheLastBlock() {
        val owners = coverageOf(listOf(midweek, weekend), listOf(meal(DayOfWeek.MONDAY, 0, "Breakfast")))

        assertEquals("weekend", owners.getValue("MON-Breakfast"))
    }

    @Test
    fun oneBlockOwnsTheWholeWeek() {
        val owners = coverageOf(listOf(midweek), fullWeek())

        assertEquals(21, owners.size)
        assertTrue(owners.values.all { it == "midweek" })
    }

    @Test
    fun noBlocksCoverNothing() {
        assertEquals(emptyMap(), coverageOf(emptyList(), fullWeek()))
    }

    @Test
    fun deletingTheBoundaryMealHandsTheBoundaryToTheNextOne() {
        val withoutSundayLunch = fullWeek().filterNot { it.id == "SUN-Lunch" }
        val owners = coverageOf(listOf(midweek, weekend), withoutSundayLunch)

        assertEquals("midweek", owners.getValue("SUN-Breakfast"))
        assertEquals("weekend", owners.getValue("SUN-Dinner"))
    }

    @Test
    fun mealsCoveredByReturnsThemInEatingOrder() {
        val covered = mealsCoveredBy(weekend, listOf(midweek, weekend), fullWeek())

        assertEquals("SUN-Lunch", covered.first().id)
        assertEquals("WED-Dinner", covered.last().id)
        assertEquals(11, covered.size, "rest of Sunday (2) + Mon/Tue/Wed (9)")
    }
}
