package com.example.mealomat.domain

import com.example.mealomat.data.db.Plan
import kotlinx.datetime.LocalDate

// Finds the version owning a slot: the latest one that had already started, null before the first.
fun planFor(slot: Slot, plans: List<Plan>): Plan? =
    plans.filter { it.activeFrom() <= slot }.maxByOrNull { it.activeFrom() }

fun Plan.activeFrom() = Slot(LocalDate.parse(active_from_date), active_from_position.toInt())
