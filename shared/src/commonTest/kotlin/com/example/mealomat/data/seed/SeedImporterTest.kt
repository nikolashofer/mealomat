package com.example.mealomat.data.seed

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PlanRepository
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.domain.Slot
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeedImporterTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var plan: PlanRepository
    private lateinit var prepBlocks: PrepBlockRepository
    private lateinit var importer: SeedImporter
    private val clock = FixedClock()
    private val activeFrom = Slot(LocalDate(2026, 6, 22), 0)

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        val outbox = OutboxWriter(db, clock)
        val auth = FakeAuth("user-1")
        val ingredients = IngredientRepository(db, outbox, auth, clock)
        plan = PlanRepository(db, outbox, auth, clock)
        prepBlocks = PrepBlockRepository(db, outbox, auth, clock)
        importer = SeedImporter(ingredients, plan, prepBlocks)
    }

    private fun planId() = plan.plans().single().id

    private fun oats() = SeedIngredient(
        key = "oats", name = "Haferflocken", basis = Basis.G100,
        kcal = 372.0, proteinG = 13.5, carbsG = 58.7, fatG = 7.0, fiberG = 10.0,
    )

    private fun seed(
        ingredients: List<SeedIngredient> = listOf(oats()),
        prepBlocks: List<SeedPrepBlock> = emptyList(),
        planMeals: List<SeedMeal> = emptyList(),
    ) = Seed(1, ingredients, prepBlocks, planMeals)

    private fun breakfast(vararg items: SeedItem) =
        listOf(SeedMeal(1, "Breakfast", 0, items.toList()))

    @Test
    fun resolvesSeedKeysToGeneratedIds() = runTest {
        importer.import(seed(planMeals = breakfast(SeedItem("oats", 80.0))), activeFrom)

        val item = plan.items(planId()).single()
        val oats = db.ingredientQueries.listWithArchived().executeAsList().single()
        assertEquals(oats.id, item.ingredient_id, "the seed key must become the real id")
        assertEquals(plan.meals(planId()).single().id, item.plan_meal_id)
    }

    @Test
    fun everySeededRowIsQueuedForSync() = runTest {
        importer.import(seed(planMeals = breakfast(SeedItem("oats", 80.0))), activeFrom)

        assertEquals(4L, db.syncOutboxQueries.count().executeAsOne(), "ingredient, plan, meal, item")
    }

    @Test
    fun aPartialSeedIsValid() = runTest {
        importer.import(Seed(version = 1, ingredients = listOf(oats())), activeFrom)

        assertEquals(1, db.ingredientQueries.listWithArchived().executeAsList().size)
        assertTrue(plan.meals(planId()).isEmpty())
    }

    @Test
    fun seededItemsHaveNoPrepModeOrComponent() = runTest {
        importer.import(seed(planMeals = breakfast(SeedItem("oats", 80.0))), activeFrom)

        val item = plan.items(planId()).single()
        assertNull(item.prep_mode, "NULL means inherit; with no component that is FRESH")
        assertNull(item.plan_component_id)
    }

    @Test
    fun itemPositionsFollowSeedOrder() = runTest {
        importer.import(
            seed(
                ingredients = listOf(oats(), oats().copy(key = "skyr", name = "Skyr")),
                planMeals = breakfast(SeedItem("skyr", 250.0), SeedItem("oats", 25.0)),
            ),
            activeFrom,
        )

        val items = plan.items(planId()).sortedBy { it.position }
        assertEquals(listOf(0L, 1L), items.map { it.position })
        assertEquals(250.0, items.first().amount)
    }

    @Test
    fun prepBlockWeekdaysSurviveTheRoundTrip() = runTest {
        importer.import(
            seed(prepBlocks = listOf(
                SeedPrepBlock("weekend", "Weekend", prepWeekday = 7, shoppingWeekday = 6,
                    coversFromWeekday = 7, coversFromPosition = 1),
            )),
            activeFrom,
        )

        val block = prepBlocks.blocks().single()
        assertEquals(DayOfWeek.SUNDAY, block.prep_weekday)
        assertEquals(DayOfWeek.SATURDAY, block.shopping_weekday)
        assertEquals(DayOfWeek.SUNDAY, block.covers_from_weekday)
        assertEquals(1L, block.covers_from_position)
    }

    @Test
    fun theSeededPlanStartsWhereItWasTold() = runTest {
        importer.import(seed(planMeals = breakfast(SeedItem("oats", 80.0))), activeFrom)

        assertEquals(planId(), plan.planAt(activeFrom)?.id)
        assertNull(plan.planAt(Slot(LocalDate(2026, 6, 21), 0)), "before the seed there is no plan")
    }

    @Test
    fun anUnknownIngredientKeyFailsLoudly() = runTest {
        val failure = runCatching {
            importer.import(seed(planMeals = breakfast(SeedItem("nope", 1.0))), activeFrom)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException, "got $failure")
        assertTrue(failure.message!!.contains("nope"))
    }
}
