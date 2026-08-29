package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

// what one prep block covers, so adjacent windows tile without overlap.
data class Window(val from: Slot, val to: Slot)

data class Boundary(val slot: Slot, val blockId: String)

operator fun Window.contains(slot: Slot) = slot in from..<to

// every date the window touches, including the partial ones at each end.
fun Window.dates(): List<LocalDate> =
    (0..from.date.daysUntil(to.date)).map { from.date.plus(it, DateTimeUnit.DAY) }

// every block boundary in the two weeks from `from`. long enough to hold any block's own and the next.
fun boundariesFrom(from: LocalDate, blocks: List<Prep_block>): List<Boundary> =
    (0..13).flatMap { offset ->
        val date = from.plus(offset, DateTimeUnit.DAY)
        blocks.filter { it.covers_from_weekday.isoDayNumber == date.dayOfWeek.isoDayNumber }
            .map { Boundary(Slot(date, it.covers_from_position.toInt()), it.id) }
    }.sortedBy { it.slot }

// the blocks next window at or after `from`: its own boundary up to whichever block opens next.
fun windowOf(block: Prep_block, blocks: List<Prep_block>, from: LocalDate): Window? {
    val boundaries = boundariesFrom(from, blocks)
    val start = boundaries.indexOfFirst { it.blockId == block.id }.takeIf { it >= 0 } ?: return null
    val end = boundaries.getOrNull(start + 1) ?: return null
    return Window(boundaries[start].slot, end.slot)
}
