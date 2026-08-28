package com.example.mealomat.domain

import kotlinx.datetime.LocalDate

data class Slot(val date: LocalDate, val position: Int) : Comparable<Slot> {
    override fun compareTo(other: Slot): Int =
        compareValuesBy(this, other, { it.date }, { it.position })
}
