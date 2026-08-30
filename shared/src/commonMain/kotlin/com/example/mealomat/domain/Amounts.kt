package com.example.mealomat.domain

import com.example.mealomat.data.db.Basis
import kotlin.math.roundToLong

fun amountLabel(amount: Double, basis: Basis): String = when (basis) {
    Basis.G100 -> "${amount.trimmed()} g"
    Basis.ML100 -> "${amount.trimmed()} ml"
    Basis.UNIT -> "${amount.trimmed()} " + if (amount == 1.0) "unit" else "units"
}

private fun Double.trimmed(): String =
    if (this == roundToLong().toDouble()) roundToLong().toString() else toString()
