package com.example.mealomat.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WeekDatesTest {

    @Test
    fun mondayIsItsOwnWeekStart() {
        val monday = LocalDate(2026, 6, 22)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(monday, weekStart(monday))
    }

    @Test
    fun sundayBelongsToTheWeekThatStartedSixDaysEarlier() {
        val sunday = LocalDate(2026, 6, 28)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate(2026, 6, 22), weekStart(sunday))
    }

    @Test
    fun weeksCrossYearBoundaries() {
        assertEquals(LocalDate(2025, 12, 29), weekStart(LocalDate(2026, 1, 1)))
        assertEquals(LocalDate(2025, 12, 29), weekStart(LocalDate(2025, 12, 31)))
    }

    @Test
    fun datesOfIsSevenConsecutiveDaysFromMonday() {
        val dates = datesOf(LocalDate(2026, 6, 22))
        assertEquals(7, dates.size)
        assertEquals(LocalDate(2026, 6, 22), dates.first())
        assertEquals(LocalDate(2026, 6, 28), dates.last())
        assertEquals(DayOfWeek.entries, dates.map { it.dayOfWeek })
    }
}
