package com.example.mealomat.feature.logbook

import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.PrepStep
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTileTest {

    private fun need(ingredientId: String, buy: Double) =
        IngredientNeed(ingredientId = ingredientId, need = 500.0, have = 500.0 - buy, buy = buy)

    private fun step(ingredientId: String) = Shopping_step(
        id = "step-$ingredientId", user_id = "user-1", shopping_trip_id = "trip-1",
        ingredient_id = ingredientId, updated_at = 0, deleted_at = null, skipped_at = null,
        needed_amount = 500.0, have_amount = 0.0, suggested_amount = 500.0, bought_amount = 500.0,
    )

    private fun prepStep(key: String, doneAt: Long?) =
        PrepStep(key = key, label = key, amount = 300.0, items = emptyList(), doneAt = doneAt)

    @Test
    fun beforeATripNothingIsDoneAndOnlyMissingThingsCount() {
        val tile = shoppingTile(
            blockId = "midweek",
            needs = listOf(need("rice", buy = 500.0), need("oats", buy = 0.0)),
            steps = emptyList(),
        )

        assertEquals(0, tile.done)
        assertEquals(1, tile.total, "the oats are already in the pantry")
    }

    @Test
    fun theTotalHoldsStillAsThingsMoveFromRemainingToDone() {
        val before = shoppingTile("midweek", listOf(need("rice", 500.0), need("egg", 6.0)), emptyList())
        val after = shoppingTile("midweek", listOf(need("egg", 6.0)), listOf(step("rice")))

        assertEquals(0 to 2, before.done to before.total)
        assertEquals(1 to 2, after.done to after.total, "one bought, the same two in total")
    }

    @Test
    fun aSkippedStepCountsAsDone() {
        val skipped = step("rice").copy(bought_amount = null, skipped_at = 5)
        val tile = shoppingTile("midweek", listOf(need("egg", 6.0)), listOf(skipped))

        assertEquals(1, tile.done, "decided against is still decided")
    }

    @Test
    fun prepCountsFinishedSteps() {
        val tile = prepTile("midweek", listOf(prepStep("rice", doneAt = 5), prepStep("sauce", null)))

        assertEquals(1, tile.done)
        assertEquals(2, tile.total)
    }

    @Test
    fun aWindowWithNothingToDoIsZeroOfZero() {
        assertEquals(0 to 0, shoppingTile("midweek", emptyList(), emptyList()).let { it.done to it.total })
        assertEquals(0 to 0, prepTile("midweek", emptyList()).let { it.done to it.total })
    }
}
