package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.LocalDate

// Where a new plan version may start: the first coverage boundary at or after `from` whose window
// nothing is committed to. Changes apply from next cycle.
fun earliestActivation(
    from: LocalDate,
    blocks: List<Prep_block>,
    committed: List<Window>,
): Slot {
    val boundaries = boundariesFrom(from, blocks).map { it.slot }
    if (boundaries.isEmpty()) return Slot(from, 0)

    return boundaries.firstOrNull { boundary ->
        val next = boundaries.firstOrNull { it > boundary }
        committed.none { it.reachesInto(boundary, next) }
    } ?: boundaries.last()
}

// `until` is null only past the end of the scan, where everything from `from` on counts.
private fun Window.reachesInto(from: Slot, until: Slot?) = to > from && (until == null || this.from < until)
