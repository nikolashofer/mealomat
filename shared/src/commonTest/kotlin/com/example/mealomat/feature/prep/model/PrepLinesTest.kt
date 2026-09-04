package com.example.mealomat.feature.prep.model

import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.domain.PrepStep
import com.example.mealomat.domain.PrepStepItem
import com.example.mealomat.domain.PrepStepTotal
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrepLinesTest {

    private val monday = LocalDate(2026, 6, 22)
    private val tuesday = LocalDate(2026, 6, 23)

    private fun ingredient(id: String, name: String, basis: Basis = Basis.G100) = Ingredient(
        id = id, user_id = "user-1", updated_at = 0, deleted_at = null,
        name = name, basis = basis, kcal = 100.0,
        protein_g = 0.0, carbs_g = 0.0, fat_g = 0.0,
        fiber_g = null, sugar_g = null, saturated_fat_g = null, salt_g = null,
        pack_size = null, archived = false, note = null,
    )

    private val library = mapOf(
        "quark" to ingredient("quark", "Magerquark"),
        "oats" to ingredient("oats", "Haferflocken"),
        "rice" to ingredient("rice", "Reis"),
        "egg" to ingredient("egg", "Ei", basis = Basis.UNIT),
    )

    private fun item(date: LocalDate, ingredientId: String, amount: Double) =
        PrepStepItem(date, "$ingredientId-$date", ingredientId, amount, preppedAt = null)

    private fun step(
        key: String,
        totals: List<PrepStepTotal>,
        items: List<PrepStepItem>,
        doneAt: Long? = null,
    ) = PrepStep(key, label = key, totals = totals, items = items, doneAt = doneAt)

    @Test
    fun aSingleIngredientStepMakesItsOwnTotal() {
        val lines = prepLines(
            listOf(
                step(
                    "ingredient:rice",
                    totals = listOf(PrepStepTotal("rice", 600.0)),
                    items = listOf(item(monday, "rice", 300.0), item(tuesday, "rice", 300.0)),
                ),
            ),
            library,
        )

        assertEquals("600 g", lines.single().make.text)
        assertTrue(lines.single().ingredients.size == 1, "one ingredient means no recipe list")
    }

    @Test
    fun aComponentMakesBoxesInsteadOfAnAmount() {
        val lines = prepLines(
            listOf(
                step(
                    "component:breakfast-base",
                    totals = listOf(PrepStepTotal("quark", 500.0), PrepStepTotal("oats", 50.0)),
                    items = listOf(
                        item(monday, "quark", 250.0), item(monday, "oats", 25.0),
                        item(tuesday, "quark", 250.0), item(tuesday, "oats", 25.0),
                    ),
                ),
            ),
            library,
        )

        assertEquals("2 boxes", lines.single().make.text, "a mixed batch has no single amount")
        assertEquals(listOf("Magerquark", "Haferflocken"), lines.single().ingredients.map { it.name })
    }

    @Test
    fun oneBasisMeansPortionsCarryAnAmountAndAShare() {
        val lines = prepLines(
            listOf(
                step(
                    "ingredient:rice",
                    totals = listOf(PrepStepTotal("rice", 450.0)),
                    // Tuesday is the small one.
                    items = listOf(item(monday, "rice", 300.0), item(tuesday, "rice", 150.0)),
                ),
            ),
            library,
        )

        val portions = lines.single().portions
        assertEquals(listOf("300 g", "150 g"), portions.map { it.amount?.text })
        assertEquals(listOf(1f, 0.5f), portions.map { it.share })
        assertEquals(listOf("MON", "TUE"), portions.map { it.weekday })
    }

    @Test
    fun mixedBasesLeavePortionsUnnumberedAtFullShare() {
        val lines = prepLines(
            listOf(
                step(
                    "component:lunch-base",
                    totals = listOf(PrepStepTotal("egg", 4.0), PrepStepTotal("quark", 400.0)),
                    items = listOf(
                        item(monday, "egg", 2.0), item(monday, "quark", 200.0),
                        item(tuesday, "egg", 2.0), item(tuesday, "quark", 200.0),
                    ),
                ),
            ),
            library,
        )

        val portions = lines.single().portions
        portions.forEach { assertNull(it.amount, "units and grams do not add up") }
        assertEquals(listOf(1f, 1f), portions.map { it.share }, "so every tile stands full height")
    }

    @Test
    fun aPreppedStepIsDone() {
        val lines = prepLines(
            listOf(
                step(
                    "ingredient:rice",
                    totals = listOf(PrepStepTotal("rice", 300.0)),
                    items = listOf(item(monday, "rice", 300.0)),
                    doneAt = 5,
                ),
            ),
            library,
        )

        assertEquals(PrepLineState.Done, lines.single().state)
        assertEquals("1 box", lines.single().portions.size.let { "$it box" })
    }
}
