package com.example.mealomat.domain

import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.Plan_item

// what a tick owes the pantry, or null if it owes nothing.
fun cookDeduction(recorded: Day_item?, planItem: Plan_item): IngredientUse? = when {
    recorded?.prepped_at != null -> null   // it already left at prep time
    recorded?.excluded == true -> null     // planned eat-out: never bought, never prepped
    recorded?.ticked_at != null -> null    // a second tap must not deduct twice
    else -> IngredientUse(planItem.ingredient_id, planItem.amount)
}

// the delta an ADJUST writes to reach the stated real amount.
fun adjustDelta(current: Double, real: Double): Double = real - current
