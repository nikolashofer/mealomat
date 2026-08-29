package com.example.mealomat.data.sync

// table names as they appear in sync_outbox and in postgres.
object Tables {
    const val INGREDIENT = "ingredient"
    const val PLAN = "plan"
    const val PLAN_MEAL = "plan_meal"
    const val PLAN_COMPONENT = "plan_component"
    const val PLAN_ITEM = "plan_item"
    const val PREP_BLOCK = "prep_block"
    const val PREP_STEP_OVERRIDE = "prep_step_override"
    const val DAY_ITEM = "day_item"
    const val PANTRY_LEDGER = "pantry_ledger"
    const val SHOPPING_TRIP = "shopping_trip"
    const val SHOPPING_STEP = "shopping_step"
    const val PREP_SESSION = "prep_session"

    // pantry_stock is absent on purpose: it is a local derived cache of pantry_ledger and never syncs.
}
