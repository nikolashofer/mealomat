package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.PrepMode
import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.earliestActivation
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrepRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var plan: PlanRepository
    private lateinit var days: DayRepository
    private lateinit var pantry: PantryRepository
    private lateinit var ingredients: IngredientRepository
    private lateinit var prepBlocks: PrepBlockRepository
    private lateinit var prep: PrepRepository
    private val clock = FixedClock()

    private val thursday = LocalDate(2026, 6, 25) // the Midweek boundary
    private val friday = LocalDate(2026, 6, 26)
    private val saturday = LocalDate(2026, 6, 27)
    private val sunday = LocalDate(2026, 6, 28) // the Weekend boundary, at position 1

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        val outbox = OutboxWriter(db, clock)
        val auth = FakeAuth("user-1")
        plan = PlanRepository(db, outbox, auth, clock)
        pantry = PantryRepository(db, outbox, auth, clock)
        ingredients = IngredientRepository(db, outbox, auth, clock)
        prepBlocks = PrepBlockRepository(db, outbox, auth, clock)
        days = DayRepository(db, outbox, auth, clock, plan, pantry)
        prep = PrepRepository(db, outbox, auth, clock, days, ingredients, prepBlocks)
    }

    private suspend fun blocks(): String {
        val midweek = prepBlocks.upsert(
            PrepBlockDraft(
                name = "Midweek", prepWeekday = DayOfWeek.WEDNESDAY, shoppingWeekday = DayOfWeek.WEDNESDAY,
                coversFromWeekday = DayOfWeek.THURSDAY, coversFromPosition = 0,
            ),
        )
        prepBlocks.upsert(
            PrepBlockDraft(
                name = "Weekend", prepWeekday = DayOfWeek.SUNDAY, shoppingWeekday = DayOfWeek.SATURDAY,
                coversFromWeekday = DayOfWeek.SUNDAY, coversFromPosition = 1,
            ),
        )
        return midweek
    }

    // rice at lunch on Friday and Saturday, prepped ahead; a fresh salad beside it on Friday.
    private suspend fun planWithPreppedRice(amount: Double = 300.0) {
        ingredients.upsert(
            IngredientDraft(id = "rice", name = "Rice", basis = Basis.G100, kcal = 350.0, proteinG = 7.0, carbsG = 78.0, fatG = 1.0),
        )
        ingredients.upsert(
            IngredientDraft(id = "salad", name = "Salad", basis = Basis.G100, kcal = 20.0, proteinG = 1.0, carbsG = 3.0, fatG = 0.0),
        )
        val v1 = plan.create(Slot(thursday, 0))
        listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach { weekday ->
            val meal = plan.upsertMeal(v1, PlanMealDraft(weekday = weekday, name = "Lunch", position = 0))
            plan.upsertItem(
                v1,
                PlanItemDraft(planMealId = meal, ingredientId = "rice", amount = amount, position = 0, prepMode = PrepMode.PREP),
            )
            if (weekday == DayOfWeek.FRIDAY) {
                plan.upsertItem(
                    v1,
                    PlanItemDraft(planMealId = meal, ingredientId = "salad", amount = 80.0, position = 1, prepMode = PrepMode.FRESH),
                )
            }
        }
    }

    @Test
    fun aStepCombinesTheSameIngredientAcrossTheWindow() = runTest {
        val midweek = blocks()
        planWithPreppedRice()

        val steps = prep.stepsFor(midweek, thursday)

        assertEquals(1, steps.size, "the fresh salad is not a step")
        assertEquals("Rice", steps.single().label)
        assertEquals(600.0, steps.single().amount)
    }

    @Test
    fun completingAStepMarksEveryLineAndDeductsEachOne() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val session = prep.forBlock(midweek, thursday)

        prep.completeStep(session, "ingredient:rice")

        assertEquals(-600.0, pantry.amountOf("rice"))
        val movements = pantry.movementsOf("rice")
        assertEquals(2, movements.size, "one row per line it covers")
        assertTrue(movements.all { it.reason == LedgerReason.PREP && it.source_kind == LedgerSource.DAY_ITEM })
        assertTrue(days.byDate(friday)!!.meals.single().items.first { it.ingredientId == "rice" }.isReady)
        assertNotNull(prep.stepsOf(session).single().doneAt)
    }

    @Test
    fun completingAStepTwiceDeductsOnce() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val session = prep.forBlock(midweek, thursday)

        prep.completeStep(session, "ingredient:rice")
        prep.completeStep(session, "ingredient:rice")

        assertEquals(-600.0, pantry.amountOf("rice"))
        assertEquals(2, pantry.movementsOf("rice").size)
    }

    @Test
    fun preppingThenTickingDeductsExactlyOnce() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val session = prep.forBlock(midweek, thursday)
        prep.completeStep(session, "ingredient:rice")
        val line = days.byDate(friday)!!.meals.single().items.first { it.ingredientId == "rice" }

        days.tickOff(friday, line.planItemId)

        assertEquals(-600.0, pantry.amountOf("rice"), "the tick adds nothing: it left at prep time")
    }

    @Test
    fun aSessionCommitsItsWindowAndAbandoningReleasesIt() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val session = prep.forBlock(midweek, thursday)

        assertEquals(
            Slot(sunday, 1),
            earliestActivation(thursday, prepBlocks.list(), prep.committedWindows(thursday)),
            "the plan cannot change inside a window being prepped",
        )

        prep.abandon(session)
        assertTrue(prep.committedWindows(thursday).isEmpty())
    }

    @Test
    fun aForgottenSessionWithNothingPreppedIsAbandoned() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val forgotten = prep.forBlock(midweek, thursday)

        val weekend = prepBlocks.list().single { it.name == "Weekend" }.id
        prep.forBlock(weekend, thursday)

        assertEquals(SessionStatus.ABANDONED, db.prepSessionQueries.findById(forgotten).executeAsOne().status)
    }

    @Test
    fun aForgottenSessionWithPreppedFoodIsCompleted() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val forgotten = prep.forBlock(midweek, thursday)
        prep.completeStep(forgotten, "ingredient:rice")

        val weekend = prepBlocks.list().single { it.name == "Weekend" }.id
        prep.forBlock(weekend, thursday)

        assertEquals(SessionStatus.DONE, db.prepSessionQueries.findById(forgotten).executeAsOne().status)
        assertEquals(-600.0, pantry.amountOf("rice"), "the food stays out of the pantry")
    }

    @Test
    fun askingTwiceContinuesTheSameSession() = runTest {
        val midweek = blocks()
        planWithPreppedRice()

        assertEquals(prep.forBlock(midweek, thursday), prep.forBlock(midweek, thursday))
    }

    @Test
    fun reorderingChangesTheWalkNotThePlan() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        ingredients.upsert(
            IngredientDraft(id = "beans", name = "Beans", basis = Basis.G100, kcal = 90.0, proteinG = 6.0, carbsG = 15.0, fatG = 1.0),
        )
        val v1 = plan.list().single().id
        val meal = plan.mealsOf(v1).single { it.weekday == DayOfWeek.SATURDAY }.id
        plan.upsertItem(
            v1,
            PlanItemDraft(planMealId = meal, ingredientId = "beans", amount = 200.0, position = 1, prepMode = PrepMode.PREP),
        )
        assertEquals(listOf("Beans", "Rice"), prep.stepsFor(midweek, thursday).map { it.label }, "by label")
        val before = plan.itemsOf(v1).map { it.id to it.updated_at }

        prep.reorder(midweek, listOf("ingredient:rice", "ingredient:beans"))

        assertEquals(listOf("Rice", "Beans"), prep.stepsFor(midweek, thursday).map { it.label })
        assertEquals(before, plan.itemsOf(v1).map { it.id to it.updated_at }, "the plan is untouched")
    }

    @Test
    fun aSessionStoresNoSteps() = runTest {
        val midweek = blocks()
        planWithPreppedRice()
        val session = prep.forBlock(midweek, thursday)

        prep.completeStep(session, "ingredient:rice")

        assertEquals(1L, db.prepSessionQueries.listCommitted(thursday.toString()).executeAsList().size.toLong())
        assertEquals(2, db.dayItemQueries.listForDate(friday.toString()).executeAsList().size + 1, "day_item rows only")
        assertNull(prep.stepsOf(session).single().lines.firstOrNull { it.preppedAt == null })
    }

    @Test
    fun aSessionOnlyCoversItsOwnWindow() = runTest {
        val midweek = blocks()
        planWithPreppedRice()

        val weekend = prepBlocks.list().single { it.name == "Weekend" }.id
        assertEquals(emptyList(), prep.stepsFor(weekend, saturday), "Sunday lunch onward holds nothing yet")
    }
}
