package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.LocalDate

// Finds where a new plan version start: the first coverage boundary at or after `from` whose
// window nothing is committed to, so changes apply from the next cycle.
fun earliestPlanActivation(
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

private fun Window.reachesInto(from: Slot, until: Slot?) = to > from && (until == null || this.from < until)
