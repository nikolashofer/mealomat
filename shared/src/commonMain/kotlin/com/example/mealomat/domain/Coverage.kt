package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan_meal
import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

// (weekday, position) packs into one comparable slot; no day holds this many meals.
private const val SLOTS_PER_DAY = 1_000L
private const val SLOTS_PER_WEEK = 7 * SLOTS_PER_DAY

// TODO: think about what should happen when there are no meals in a day, prolly just make covers_from_position nullable, so if nul it implicitly starts at the beginning of the day

// meal id -> owning block id. A block owns every meal from its boundary to the next blocks, walking
// the week cyclically, so the windows tile the plan exactly. Coverage is contiguous.
fun coverageOf(blocks: List<Prep_block>, meals: List<Plan_meal>): Map<String, String> {
    if (blocks.isEmpty()) return emptyMap()
    val ordered = blocks.sortedBy { it.boundary() }
    return meals.associate { meal ->
        val slot = slotOf(meal.weekday, meal.position)
        val owner = ordered.lastOrNull { it.boundary() <= slot } ?: ordered.last()
        meal.id to owner.id
    }
}

// One blocks meals in eating order, sorted from its own boundary. A window that wraps the week
// (Sunday to Wednesday) is not in slot order.
fun mealsCoveredBy(block: Prep_block, blocks: List<Prep_block>, meals: List<Plan_meal>): List<Plan_meal> {
    val coverage = coverageOf(blocks, meals)
    val start = block.boundary()
    return meals.filter { coverage[it.id] == block.id }
        .sortedBy { (slotOf(it.weekday, it.position) - start).mod(SLOTS_PER_WEEK) }
}

private fun Prep_block.boundary() = slotOf(covers_from_weekday, covers_from_position)

// 0-based on monday, so slots span [0, SLOTS_PER_WEEK) and the rotation above is exact.
private fun slotOf(weekday: DayOfWeek, position: Long) =
    (weekday.isoDayNumber - 1) * SLOTS_PER_DAY + position
