package com.example.mealomat.data.sync

// table names as they appear in sync_outbox and in postgres.
object Tables {
    const val INGREDIENT = "ingredient"
    const val PLAN_MEAL = "plan_meal"
    const val PLAN_COMPONENT = "plan_component"
    const val PLAN_ITEM = "plan_item"
    const val PREP_BLOCK = "prep_block"
    const val PREP_STEP_OVERRIDE = "prep_step_override"
}
