package com.example.mealomat.data.seed

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PlanRepository
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeedImporterTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var ingredients: IngredientRepository
    private lateinit var plan: PlanRepository
    private lateinit var prepBlocks: PrepBlockRepository
    private lateinit var importer: SeedImporter
    private val clock = FixedClock()

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        val outbox = OutboxWriter(db, clock)
        val auth = FakeAuth("user-1")
        ingredients = IngredientRepository(db, outbox, auth, clock)
        plan = PlanRepository(db, outbox, auth, clock)
        prepBlocks = PrepBlockRepository(db, outbox, auth, clock)
        importer = SeedImporter(ingredients, plan, prepBlocks)
    }

    private fun oats() = SeedIngredient(
        key = "oats", name = "Haferflocken", basis = Basis.G100,
        kcal = 372.0, proteinG = 13.5, carbsG = 58.7, fatG = 7.0, fiberG = 10.0,
    )

    private fun seed(
        ingredients: List<SeedIngredient> = listOf(oats()),
        prepBlocks: List<SeedPrepBlock> = emptyList(),
        planMeals: List<SeedMeal> = emptyList(),
    ) = Seed(1, ingredients, prepBlocks, planMeals)

    @Test
    fun resolvesSeedKeysToGeneratedIds() = runTest {
        importer.import(
            seed(planMeals = listOf(
                SeedMeal(1, "Breakfast", 0, listOf(SeedItem("oats", 80.0))),
            )),
        )

        val item = plan.items().single()
        val oats = db.ingredientQueries.listWithArchived().executeAsList().single()
        assertEquals(oats.id, item.ingredient_id, "the seed key must become the real id")
        assertEquals(plan.meals().single().id, item.plan_meal_id)
        assertTrue(oats.id.contains("-"), "ids are UUIDs, not seed keys")
    }
    
    @Test
    fun everySeededRowIsQueuedForSync() = runTest {
        importer.import(
            seed(planMeals = listOf(SeedMeal(1, "Breakfast", 0, listOf(SeedItem("oats", 80.0))))),
        )

        assertEquals(3L, db.syncOutboxQueries.count().executeAsOne())
    }

    @Test
    fun aPartialSeedIsValid() = runTest {
        importer.import(Seed(version = 1, ingredients = listOf(oats())))

        assertEquals(1, db.ingredientQueries.listWithArchived().executeAsList().size)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun seededItemsHaveNoPrepModeOrComponent() = runTest {
        importer.import(
            seed(planMeals = listOf(SeedMeal(1, "Breakfast", 0, listOf(SeedItem("oats", 80.0))))),
        )

        val item = plan.items().single()
        assertNull(item.prep_mode)
        assertNull(item.plan_component_id)
    }

    @Test
    fun itemPositionsFollowSeedOrder() = runTest {
        importer.import(
            seed(
                ingredients = listOf(oats(), oats().copy(key = "skyr", name = "Skyr")),
                planMeals = listOf(SeedMeal(1, "Breakfast", 0, listOf(
                    SeedItem("skyr", 250.0), SeedItem("oats", 25.0),
                ))),
            ),
        )

        assertEquals(listOf(0L, 1L), plan.items().sortedBy { it.position }.map { it.position })
        assertEquals(250.0, plan.items().single { it.position == 0L }.amount)
    }

    @Test
    fun prepBlockWeekdaysSurviveTheRoundTrip() = runTest {
        importer.import(
            seed(prepBlocks = listOf(
                SeedPrepBlock("weekend", "Weekend", prepWeekday = 7, shoppingWeekday = 6,
                    coversFromWeekday = 7, coversFromPosition = 1),
            )),
        )

        val block = prepBlocks.blocks().single()
        assertEquals(DayOfWeek.SUNDAY, block.prep_weekday)
        assertEquals(DayOfWeek.SATURDAY, block.shopping_weekday)
        assertEquals(DayOfWeek.SUNDAY, block.covers_from_weekday)
        assertEquals(1L, block.covers_from_position)
    }

    @Test
    fun anUnknownIngredientKeyFailsLoudly() = runTest {
        val failure = runCatching {
            importer.import(seed(planMeals = listOf(
                SeedMeal(1, "Breakfast", 0, listOf(SeedItem("nope", 1.0))),
            )))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException, "got $failure")
        assertTrue(failure.message!!.contains("nope"))
    }
}
