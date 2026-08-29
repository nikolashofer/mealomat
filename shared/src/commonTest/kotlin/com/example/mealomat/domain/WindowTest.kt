package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowTest {

    private fun block(name: String, coversFrom: DayOfWeek, position: Long) = Prep_block(
        id = name, user_id = "user-1", updated_at = 0, deleted_at = null, name = name,
        prep_weekday = coversFrom, shopping_weekday = coversFrom,
        covers_from_weekday = coversFrom, covers_from_position = position,
    )

    private val midweek = block("midweek", DayOfWeek.THURSDAY, 0)
    private val weekend = block("weekend", DayOfWeek.SUNDAY, 1)
    private val blocks = listOf(midweek, weekend)

    private val thursday = LocalDate(2026, 6, 25)
    private val sunday = LocalDate(2026, 6, 28)
    private val nextThursday = LocalDate(2026, 7, 2)

    @Test
    fun aWindowRunsToTheNextBlocksBoundary() {
        assertEquals(Window(Slot(thursday, 0), Slot(sunday, 1)), windowOf(midweek, blocks, thursday))
    }

    @Test
    fun aWindowThatWrapsTheWeekEndsOnTheRightDate() {
        assertEquals(Window(Slot(sunday, 1), Slot(nextThursday, 0)), windowOf(weekend, blocks, thursday))
    }

    @Test
    fun theOnlyBlocksWindowIsAWholeWeek() {
        assertEquals(
            Window(Slot(thursday, 0), Slot(nextThursday, 0)),
            windowOf(midweek, listOf(midweek), thursday),
        )
    }

    @Test
    fun aWindowStartsAtOrAfterTheDateAsked() {
        val friday = LocalDate(2026, 6, 26)

        assertEquals(Slot(nextThursday, 0), windowOf(midweek, blocks, friday)?.from, "not the one it is inside")
    }

    @Test
    fun theEndIsExclusiveSoWindowsTile() {
        val window = windowOf(midweek, blocks, thursday)!!

        assertTrue(Slot(thursday, 0) in window, "its own boundary is inside")
        assertTrue(Slot(sunday, 0) in window)
        assertFalse(Slot(sunday, 1) in window, "the next block's boundary belongs to that block")
    }
}
