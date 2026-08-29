package com.example.mealomat.domain

import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.Plan_item
import com.example.mealomat.data.db.PrepMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PantryTest {

    private val planItem = Plan_item(
        id = "item-1", user_id = "user-1", plan_id = "plan-1", plan_meal_id = "meal-1",
        plan_component_id = null, ingredient_id = "rice", updated_at = 0, deleted_at = null,
        amount = 180.0, position = 0, prep_mode = null,
    )

    private fun recorded(
        prepped: Long? = null,
        ticked: Long? = null,
        excluded: Boolean = false,
    ) = Day_item(
        id = "day-1", user_id = "user-1", plan_item_id = "item-1", updated_at = 0, deleted_at = null,
        prepped_at = prepped, ticked_at = ticked,
        date = "2026-06-22", excluded = excluded,
    )

    @Test
    fun anUntouchedItemDeductsItsPlannedAmount() {
        assertEquals(IngredientUse("rice", 180.0), cookDeduction(null, planItem))
        assertEquals(IngredientUse("rice", 180.0), cookDeduction(recorded(), planItem))
    }

    @Test
    fun aPreppedItemDeductsNothing() {
        assertNull(cookDeduction(recorded(prepped = 1), planItem), "it left the pantry at prep time")
    }

    @Test
    fun anExcludedItemDeductsNothing() {
        assertNull(cookDeduction(recorded(excluded = true), planItem), "never bought, never prepped")
    }

    @Test
    fun anAlreadyTickedItemDeductsNothing() {
        assertNull(cookDeduction(recorded(ticked = 1), planItem), "a second tap must not deduct twice")
    }

    @Test
    fun adjustReachesTheStatedRealAmount() {
        assertEquals(-30.0, adjustDelta(current = 100.0, real = 70.0))
        assertEquals(70.0, adjustDelta(current = 30.0, real = 100.0))
        assertEquals(120.0, adjustDelta(current = -20.0, real = 100.0), "from negative stock")
        assertEquals(0.0, adjustDelta(current = 100.0, real = 100.0))
    }

    @Test
    fun aFreshItemIsNeverPrepped() {
        assertNull(prepDeduction(null, planItem, PrepMode.FRESH), "the prep wizard does not touch it")
    }

    @Test
    fun prepDeductsThePlannedAmount() {
        assertEquals(IngredientUse("rice", 180.0), prepDeduction(null, planItem, PrepMode.PREP))
    }

    @Test
    fun prepDeductsNothingTwiceNorAfterTheFact() {
        assertNull(prepDeduction(recorded(prepped = 1), planItem, PrepMode.PREP), "already made")
        assertNull(prepDeduction(recorded(excluded = true), planItem, PrepMode.PREP), "planned eat-out")
        assertNull(prepDeduction(recorded(ticked = 1), planItem, PrepMode.PREP), "already cooked fresh")
    }
}
