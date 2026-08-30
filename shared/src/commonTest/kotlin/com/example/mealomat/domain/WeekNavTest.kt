package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeekNavTest {

    // TODO: generalize mocks somewhere used by all tests
    private fun block(name: String, shopping: DayOfWeek, prep: DayOfWeek) = Prep_block(
        id = name, user_id = "user-1", updated_at = 0, deleted_at = null, name = name,
        prep_weekday = prep, shopping_weekday = shopping,
        covers_from_weekday = prep, covers_from_position = 0,
    )

    private val blocks = listOf(
        block("Midweek", shopping = DayOfWeek.WEDNESDAY, prep = DayOfWeek.WEDNESDAY),
        block("Weekend", shopping = DayOfWeek.SATURDAY, prep = DayOfWeek.SUNDAY),
    )

    private val thursday = LocalDate(2026, 6, 25)

    @Test
    fun theWeekRunsMondayToSundayAroundToday() {
        val week = weekNav(thursday, blocks)

        assertEquals(7, week.size)
        assertEquals(LocalDate(2026, 6, 22), week.first().date)
        assertEquals(LocalDate(2026, 6, 28), week.last().date)
        assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
    }

    @Test
    fun theWeekIsTheSameWhicheverDayOfItIsToday() {
        assertEquals(
            weekNav(thursday, blocks).map { it.date },
            weekNav(LocalDate(2026, 6, 22), blocks).map { it.date },
        )
    }

    @Test
    fun onlyDaysBeforeTodayArePast() {
        val week = weekNav(thursday, blocks).associateBy { it.date.dayOfWeek }

        assertTrue(week.getValue(DayOfWeek.WEDNESDAY).isPast)
        assertFalse(week.getValue(DayOfWeek.THURSDAY).isPast, "today is not past")
        assertFalse(week.getValue(DayOfWeek.FRIDAY).isPast)
    }

    @Test
    fun aBlockLightsTheDaysItShopsAndPrepsOn() {
        val week = weekNav(thursday, blocks).associateBy { it.date.dayOfWeek }

        assertTrue(week.getValue(DayOfWeek.WEDNESDAY).let { it.shops && it.preps }, "two dots")
        assertTrue(week.getValue(DayOfWeek.SATURDAY).let { it.shops && !it.preps })
        assertTrue(week.getValue(DayOfWeek.SUNDAY).let { !it.shops && it.preps })
        assertTrue(week.getValue(DayOfWeek.MONDAY).let { !it.shops && !it.preps })
    }

    @Test
    fun withNoBlocksNoDayIsLit() {
        assertTrue(weekNav(thursday, emptyList()).none { it.shops || it.preps })
    }
}
