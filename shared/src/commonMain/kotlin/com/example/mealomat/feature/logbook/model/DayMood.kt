package com.example.mealomat.feature.logbook.model

import com.example.mealomat.domain.DayTotals
import com.example.mealomat.ui.components.Mascot
import kotlin.math.roundToInt

private val Mascots = listOf(
    0 to Mascot.Thinking,
    1 to Mascot.Happy,
    30 to Mascot.Wink,
    55 to Mascot.Cool,
    80 to Mascot.Excited,
    100 to Mascot.Love,
)

private val Blurbs = listOf(
    0 to "Nothing yet",
    1 to "Started",
    30 to "Getting there",
    55 to "Past half",
    80 to "Nearly there",
    100 to "Done",
)

fun moodMascot(percent: Int): Mascot = pick(Mascots, percent)

fun moodBlurb(percent: Int): String = pick(Blurbs, percent)

fun eatenPercent(totals: DayTotals): Int {
    if (totals.planned.kcal <= 0.0) return 0
    return ((totals.eaten.kcal / totals.planned.kcal) * 100).roundToInt().coerceIn(0, 100)
}

private fun <T> pick(table: List<Pair<Int, T>>, percent: Int): T =
    table.last { percent >= it.first }.second
