package com.example.mealomat.domain

import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.Plan_item
import com.example.mealomat.data.db.PrepMode

// Works out what a tick owes the pantry, or null if it owes nothing.
fun cookDeduction(recorded: Day_item?, planItem: Plan_item): IngredientUse? = when {
    recorded?.prepped_at != null -> null   // it already left at prep time
    recorded?.excluded == true -> null     // planned eat-out: never bought, never prepped
    recorded?.ticked_at != null -> null    // a second tap must not deduct twice
    else -> IngredientUse(planItem.ingredient_id, planItem.amount)
}

// Works out what prepping owes the pantry, or null if it owes nothing.
fun prepDeduction(recorded: Day_item?, planItem: Plan_item, mode: PrepMode): IngredientUse? = when {
    mode == PrepMode.FRESH -> null          // the prep wizard never touches a fresh line
    recorded?.prepped_at != null -> null    // a second tap must not deduct twice
    recorded?.excluded == true -> null      // planned eat-out: not bought, not prepped
    recorded?.ticked_at != null -> null     // already cooked fresh, so it already left
    else -> IngredientUse(planItem.ingredient_id, planItem.amount)
}

// Computes the delta an ADJUST writes to reach the stated real amount.
fun adjustDelta(current: Double, real: Double): Double = real - current
