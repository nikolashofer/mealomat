package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanSelectionTest {

    private fun plan(id: String, date: LocalDate, position: Int) = Plan(
        id = id, user_id = "user-1", updated_at = 0, deleted_at = null,
        active_from_date = date.toString(), active_from_position = position.toLong(),
    )

    private val v1 = plan("v1", LocalDate(2026, 6, 22), 0)
    private val v2 = plan("v2", LocalDate(2026, 6, 28), 1) // from Sunday's second meal
    private val plans = listOf(v1, v2)

    @Test
    fun aDateBeforeEveryVersionHasNoPlan() {
        assertNull(planFor(Slot(LocalDate(2026, 6, 21), 0), plans))
    }

    @Test
    fun aDatePicksTheLatestVersionThatHadStarted() {
        assertEquals("v1", planFor(Slot(LocalDate(2026, 6, 24), 0), plans)?.id)
        assertEquals("v2", planFor(Slot(LocalDate(2026, 6, 30), 0), plans)?.id)
    }

    @Test
    fun aMidDayBoundarySplitsTheDay() {
        assertEquals("v1", planFor(Slot(LocalDate(2026, 6, 28), 0), plans)?.id, "Sunday breakfast")
        assertEquals("v2", planFor(Slot(LocalDate(2026, 6, 28), 1), plans)?.id, "Sunday lunch onwards")
    }

    @Test
    fun noVersionsMeansNoPlan() {
        assertNull(planFor(Slot(LocalDate(2026, 6, 24), 0), emptyList()))
    }
}
