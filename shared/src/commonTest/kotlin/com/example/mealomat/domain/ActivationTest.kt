package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivationTest {

    private fun block(name: String, coversFrom: DayOfWeek, position: Long) = Prep_block(
        id = name, user_id = "user-1", updated_at = 0, deleted_at = null, name = name,
        prep_weekday = coversFrom, shopping_weekday = coversFrom,
        covers_from_weekday = coversFrom, covers_from_position = position,
    )

    private val midweek = block("midweek", DayOfWeek.THURSDAY, 0)
    private val weekend = block("weekend", DayOfWeek.SUNDAY, 1)
    private val blocks = listOf(midweek, weekend)

    private val thursday = LocalDate(2026, 6, 25)
    private val friday = LocalDate(2026, 6, 26)
    private val sunday = LocalDate(2026, 6, 28)

    @Test
    fun withNothingCommittedItStartsAtTheNextBoundary() {
        assertEquals(Slot(thursday, 0), earliestActivation(thursday, blocks, emptyList()))
    }

    @Test
    fun aBoundaryCanFallMidDay() {
        assertEquals(Slot(sunday, 1), earliestActivation(friday, blocks, emptyList()))
    }

    @Test
    fun aCommittedWindowIsSkipped() {
        val committed = listOf(Window(Slot(thursday, 0), Slot(sunday, 1)))   // the Midweek window

        assertEquals(Slot(sunday, 1), earliestActivation(thursday, blocks, committed))
    }

    @Test
    fun twoCommittedWindowsInARowAreBothSkipped() {
        val committed = listOf(
            Window(Slot(thursday, 0), Slot(sunday, 1)),                          // Midweek
            Window(Slot(sunday, 1), Slot(LocalDate(2026, 7, 2), 0)),             // Weekend
        )

        val activation = earliestActivation(thursday, blocks, committed)

        assertEquals(Slot(LocalDate(2026, 7, 2), 0), activation, "the following Thursday")
    }

    @Test
    fun noBlocksMeansStartNow() {
        assertEquals(Slot(thursday, 0), earliestActivation(thursday, emptyList(), emptyList()))
    }
}
