package com.example.mealomat.domain

import com.example.mealomat.data.db.Basis
import kotlin.math.roundToInt

data class MeasureLabel(val value: String, val unit: String) {
    val text: String get() = "$value $unit"
}

fun amountLabel(amount: Double, basis: Basis): MeasureLabel = when (basis) {
    Basis.G100 -> MeasureLabel(amount.trimmed(), "g")
    Basis.ML100 -> MeasureLabel(amount.trimmed(), "ml")
    Basis.UNIT -> MeasureLabel(amount.trimmed(), if (amount == 1.0) "unit" else "units")
}

fun kcalLabel(kcal: Double): MeasureLabel = MeasureLabel(kcal.rounded(), "kcal")

fun gramsValue(grams: Double): String = grams.rounded()

// Rounds to a whole number.
private fun Double.rounded(): String = roundToInt().toString().grouped()

// Drops a trailing ".0" and leaves the rest as written (i.e. "0.5").
private fun Double.trimmed(): String =
    (if (this == roundToInt().toDouble()) roundToInt().toString() else toString()).grouped()

// Puts a space between thousands.
private fun String.grouped(): String {
    val sign = if (startsWith("-")) "-" else ""
    val digits = removePrefix("-")
    val whole = digits.substringBefore('.')
    val fraction = digits.substringAfter('.', "")
    val spaced = whole.reversed().chunked(3).joinToString(" ").reversed()
    return sign + spaced + if (fraction.isEmpty()) "" else ".$fraction"
}
