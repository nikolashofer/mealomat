package com.example.mealomat.domain

import com.example.mealomat.data.db.Prep_block
import kotlinx.datetime.LocalDate

data class DayNav(
    val date: LocalDate,
    val isPast: Boolean,
    val shops: Boolean,
    val preps: Boolean,
)

fun weekNav(today: LocalDate, blocks: List<Prep_block>): List<DayNav> =
    datesOf(weekStart(today)).map { date ->
        DayNav(
            date = date,
            isPast = date < today,
            shops = blocks.any { it.shopping_weekday == date.dayOfWeek },
            preps = blocks.any { it.prep_weekday == date.dayOfWeek },
        )
    }
