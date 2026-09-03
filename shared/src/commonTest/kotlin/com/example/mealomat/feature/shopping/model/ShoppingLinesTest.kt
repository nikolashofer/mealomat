package com.example.mealomat.feature.shopping.model

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShoppingLinesTest {

    private fun ingredient(id: String, name: String, pack: Double? = null) = Ingredient(
        id = id, user_id = "user-1", updated_at = 0, deleted_at = null,
        name = name, basis = Basis.G100, kcal = 100.0,
        protein_g = 0.0, carbs_g = 0.0, fat_g = 0.0,
        fiber_g = null, sugar_g = null, saturated_fat_g = null, salt_g = null,
        pack_size = pack, archived = false, note = null,
    )

    private fun step(ingredientId: String, bought: Double?, skipped: Long? = null) = Shopping_step(
        id = "step-$ingredientId", user_id = "user-1", shopping_trip_id = "trip-1",
        ingredient_id = ingredientId, updated_at = 0, deleted_at = null, skipped_at = skipped,
        needed_amount = 700.0, have_amount = 0.0, suggested_amount = 1000.0, bought_amount = bought,
    )

    private val library = mapOf(
        "berries" to ingredient("berries", "Frozen berry mix", pack = 500.0),
        "kiwi" to ingredient("kiwi", "Kiwi"),
        "chia" to ingredient("chia", "Chia seeds"),
    )

    @Test
    fun buyRoundsUpToWholePacks() {
        val need = IngredientNeed("berries", need = 700.0, have = 0.0, buy = 700.0, packSize = 500.0)

        assertEquals(1000.0, suggestedBuy(need), "700 g of a 500 g pack is two packs")
    }

    @Test
    fun withoutAPackSizeTheRawAmountStands() {
        val need = IngredientNeed("kiwi", need = 5.0, have = 0.0, buy = 5.0)

        assertEquals(5.0, suggestedBuy(need))
    }

    @Test
    fun aBoughtLineIsSettledEvenThoughItStillReportsANeed() {
        val lines = shoppingLines(
            needs = listOf(IngredientNeed("berries", need = 700.0, have = 0.0, buy = 700.0, packSize = 500.0)),
            steps = listOf(step("berries", bought = 1000.0)),
            ingredients = library,
        )

        assertEquals(1, lines.size, "the need must not produce a second line")
        assertEquals(LineState.Bought, lines.single().state)
        assertEquals("1 000 g", lines.single().bought?.text)
    }

    @Test
    fun aLineWithNoLiveNeedFallsBackToWhatTheStepRecorded() {
        val lines = shoppingLines(
            needs = emptyList(),
            steps = listOf(step("berries", bought = 1000.0)),
            ingredients = library,
        )

        assertEquals("700 g", lines.single().need.text, "the step snapshots what it was worth")
        assertEquals("1 000 g", lines.single().bought?.text)
    }

    @Test
    fun aSkippedLineStaysNeededButIsNotPending() {
        val lines = shoppingLines(
            needs = listOf(IngredientNeed("chia", need = 90.0, have = 0.0, buy = 90.0)),
            steps = listOf(step("chia", bought = null, skipped = 5)),
            ingredients = library,
        )

        assertEquals(1, lines.size, "the need must not produce a second line")
        assertEquals(LineState.Skipped, lines.single().state)
        assertNull(lines.single().bought)
    }

    @Test
    fun everyLineHoldsItsPlaceWhateverItsState() {
        val lines = shoppingLines(
            needs = listOf(
                IngredientNeed("chia", need = 90.0, have = 0.0, buy = 90.0),
                IngredientNeed("kiwi", need = 5.0, have = 0.0, buy = 5.0),
            ),
            steps = listOf(
                step("berries", bought = 1000.0),
                step("chia", bought = null, skipped = 5),
            ),
            ingredients = library,
        )

        assertEquals(listOf("berries", "chia", "kiwi"), lines.map { it.ingredientId })
        assertEquals(
            listOf(LineState.Bought, LineState.Skipped, LineState.Pending),
            lines.map { it.state },
            "state must not reorder anything",
        )
    }

    @Test
    fun actingOnALineDoesNotMoveIt() {
        val needs = listOf(
            IngredientNeed("berries", need = 700.0, have = 0.0, buy = 700.0, packSize = 500.0),
            IngredientNeed("chia", need = 90.0, have = 0.0, buy = 90.0),
        )
        val before = shoppingLines(needs, steps = emptyList(), ingredients = library)
        val after = shoppingLines(needs, steps = listOf(step("berries", bought = null, skipped = 5)), library)

        assertEquals(before.map { it.ingredientId }, after.map { it.ingredientId })
        assertEquals(LineState.Skipped, after.first().state)
    }

    @Test
    fun aStockedLineIsNotAStepAtAll() {
        val lines = shoppingLines(
            needs = listOf(IngredientNeed("kiwi", need = 5.0, have = 9.0, buy = 0.0)),
            steps = emptyList(),
            ingredients = library,
        )

        assertEquals(emptyList(), lines, "nothing to buy means nothing to walk through")
    }

    @Test
    fun theBuyLabelShowsThePackRoundedAmount() {
        val lines = shoppingLines(
            needs = listOf(IngredientNeed("berries", need = 700.0, have = 0.0, buy = 700.0, packSize = 500.0)),
            steps = emptyList(),
            ingredients = library,
        )

        assertEquals("1 000 g", lines.single().buy.text)
        assertEquals("700 g", lines.single().need.text, "the need itself is not rounded")
    }
}
