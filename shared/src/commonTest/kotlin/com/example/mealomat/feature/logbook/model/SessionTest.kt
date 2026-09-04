package com.example.mealomat.feature.logbook.model

import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.PrepStep
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTest {

    private fun need(ingredientId: String, buy: Double) =
        IngredientNeed(ingredientId = ingredientId, need = 500.0, have = 500.0 - buy, buy = buy)

    private fun step(ingredientId: String) = Shopping_step(
        id = "step-$ingredientId", user_id = "user-1", shopping_trip_id = "trip-1",
        ingredient_id = ingredientId, updated_at = 0, deleted_at = null, skipped_at = null,
        needed_amount = 500.0, have_amount = 0.0, suggested_amount = 500.0, bought_amount = 500.0,
    )

    private fun prepStep(key: String, doneAt: Long?) =
        PrepStep(key = key, label = key, totals = emptyList(), items = emptyList(), doneAt = doneAt)

    @Test
    fun beforeATripNothingIsDoneAndOnlyMissingThingsCount() {
        val session = shoppingSession(
            blockId = "midweek",
            status = SessionStatus.IN_PROGRESS,
            needs = listOf(need("rice", buy = 500.0), need("oats", buy = 0.0)),
            steps = emptyList(),
        )

        assertEquals(0, session.done)
        assertEquals(1, session.total, "the oats are already in the pantry")
    }

    @Test
    fun theTotalHoldsStillAsThingsMoveFromRemainingToDone() {
        val before = shoppingSession("midweek", SessionStatus.IN_PROGRESS, listOf(need("rice", 500.0), need("egg", 6.0)), emptyList())
        val after = shoppingSession("midweek", SessionStatus.IN_PROGRESS, listOf(need("egg", 6.0)), listOf(step("rice")))

        assertEquals(0 to 2, before.done to before.total)
        assertEquals(1 to 2, after.done to after.total, "one bought, the same two in total")
    }

    @Test
    fun aBoughtLineIsNotCountedTwice() {
        val session = shoppingSession(
            blockId = "midweek",
            status = SessionStatus.IN_PROGRESS,
            needs = listOf(need("rice", buy = 500.0), need("egg", buy = 6.0)),
            steps = listOf(step("rice")),
        )

        assertEquals(1 to 2, session.done to session.total, "the total must not grow as things are bought")
    }

    @Test
    fun aSkippedStepCountsAsDone() {
        val skipped = step("rice").copy(bought_amount = null, skipped_at = 5)
        val session = shoppingSession("midweek", SessionStatus.IN_PROGRESS, listOf(need("egg", 6.0)), listOf(skipped))

        assertEquals(1, session.done, "decided against is still decided")
    }

    @Test
    fun skippedIsCountedApartFromWhatWasActuallyGot() {
        val skipped = step("rice").copy(bought_amount = null, skipped_at = 5)
        val session = shoppingSession("midweek", SessionStatus.DONE, emptyList(), listOf(step("egg"), skipped))

        assertEquals(2, session.done, "both lines were settled")
        assertEquals(1, session.skipped)
        assertEquals(1, session.got, "only the egg was actually bought")
    }

    @Test
    fun prepNeverSkips() {
        val session = prepSession("midweek", SessionStatus.DONE, listOf(prepStep("rice", doneAt = 5)))

        assertEquals(0, session.skipped)
        assertEquals(1, session.got)
    }

    @Test
    fun prepCountsFinishedSteps() {
        val session = prepSession("midweek", SessionStatus.IN_PROGRESS, listOf(prepStep("rice", doneAt = 5), prepStep("sauce", null)))

        assertEquals(1, session.done)
        assertEquals(2, session.total)
    }

    @Test
    fun aWindowWithNothingToDoIsZeroOfZero() {
        assertEquals(0 to 0, shoppingSession("midweek", SessionStatus.IN_PROGRESS, emptyList(), emptyList()).let { it.done to it.total })
        assertEquals(0 to 0, prepSession("midweek", SessionStatus.IN_PROGRESS, emptyList()).let { it.done to it.total })
    }
}
