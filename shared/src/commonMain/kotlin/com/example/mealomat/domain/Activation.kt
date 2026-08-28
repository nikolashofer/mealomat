package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

// Where a new plan version may start: the first coverage boundary at or after `from` with nothing
// committed in its window. changes apply from next cycle
fun earliestActivation(
    from: LocalDate,
    blocks: List<Prep_block>,
    committed: Set<Slot>,
): Slot {
    val boundaries = upcomingBoundaries(from, blocks)
    if (boundaries.isEmpty()) return Slot(from, 0)

    return boundaries.firstOrNull { boundary ->
        val next = boundaries.firstOrNull { it > boundary }
        committed.none { it >= boundary && (next == null || it < next) }
    } ?: boundaries.last()
}

// Every block boundary in the two weeks from `from`, in order. Two weeks is enough: a window cannot
// be longer than the cycle, so the first uncommitted one is always in range.
private fun upcomingBoundaries(from: LocalDate, blocks: List<Prep_block>): List<Slot> =
    (0..13).flatMap { offset ->
        val date = from.plus(offset, DateTimeUnit.DAY)
        blocks.filter { it.covers_from_weekday.isoDayNumber == date.dayOfWeek.isoDayNumber }
            .map { Slot(date, it.covers_from_position.toInt()) }
    }.sorted()
