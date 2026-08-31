package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.earliestPlanActivation
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShoppingRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var plan: PlanRepository
    private lateinit var days: DayRepository
    private lateinit var pantry: PantryRepository
    private lateinit var ingredients: IngredientRepository
    private lateinit var prepBlocks: PrepBlockRepository
    private lateinit var shopping: ShoppingRepository
    private val clock = FixedClock()

    private val thursday = LocalDate(2026, 6, 25) // the Midweek boundary
    private val friday = LocalDate(2026, 6, 26)
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
        shopping = ShoppingRepository(db, outbox, auth, clock, days, pantry, ingredients, prepBlocks)
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

    // rice on Friday lunch, 500g, under a plan version that starts on the Midweek boundary.
    private suspend fun planWithFridayRice(amount: Double = 500.0, packSize: Double? = null) {
        ingredients.upsert(
            IngredientDraft(
                id = "rice", name = "Rice", basis = Basis.G100,
                kcal = 350.0, proteinG = 7.0, carbsG = 78.0, fatG = 1.0, packSize = packSize,
            ),
        )
        val v1 = plan.create(Slot(thursday, 0))
        val meal = plan.upsertMeal(v1, PlanMealDraft(weekday = DayOfWeek.FRIDAY, name = "Lunch", position = 0))
        plan.upsertItem(v1, PlanItemDraft(planMealId = meal, ingredientId = "rice", amount = amount, position = 0))
    }

    @Test
    fun theNeedIsWhatTheWindowPlansMinusWhatIsInThePantry() = runTest {
        val midweek = blocks()
        planWithFridayRice(amount = 500.0)
        pantry.addAhead("rice", 200.0)

        val need = shopping.needsFor(midweek, thursday).single()

        assertEquals(500.0, need.need)
        assertEquals(200.0, need.have)
        assertEquals(300.0, need.buy)
    }

    @Test
    fun aPackSizeIsCarriedAsAHintWithoutRoundingTheAmount() = runTest {
        val midweek = blocks()
        planWithFridayRice(amount = 340.0, packSize = 500.0)

        val need = shopping.needsFor(midweek, thursday).single()

        assertEquals(340.0, need.buy, "the exact shortfall, not a whole pack")
        assertEquals(500.0, need.packSize)
    }

    @Test
    fun startingWritesOnlyTheTrip() = runTest {
        val midweek = blocks()
        planWithFridayRice()

        val trip = shopping.forBlock(midweek, thursday)

        assertTrue(shopping.stepsOf(trip).isEmpty(), "a line exists because something happened to it")
        assertTrue(db.dayItemQueries.listForDate(friday.toString()).executeAsList().isEmpty())
    }

    @Test
    fun anAdjustedPantryChangesWhatIsStillNeeded() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)
        assertEquals(500.0, shopping.needsOf(trip).single().buy)

        pantry.adjustTo("rice", 200.0, note = "checked the shelf")

        assertEquals(300.0, shopping.needsOf(trip).single().buy, "the correction is not stale")
    }

    @Test
    fun buyingDoesNotMoveTheNumberUnderTheUsersThumb() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)

        shopping.buyStep(trip, "rice", 500.0)

        val need = shopping.needsOf(trip).single()
        assertEquals(0.0, need.have, "the trip's own purchase is netted out")
        assertEquals(500.0, need.buy, "so what it asked for has not changed")
    }

    @Test
    fun buyingAddsExactlyWhatWasEntered() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)

        shopping.buyStep(trip, "rice", 800.0)

        assertEquals(800.0, pantry.amountOf("rice"))
        val movement = pantry.movementsOf("rice").single()
        assertEquals(LedgerReason.BUY, movement.reason)
        assertEquals(LedgerSource.SHOPPING_STEP, movement.source_kind)
        assertEquals(shopping.stepsOf(trip).single().id, movement.source_id)
    }

    @Test
    fun anActionRecordsTheNumbersItWasShown() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        pantry.addAhead("rice", 200.0)
        val trip = shopping.forBlock(midweek, thursday)

        shopping.buyStep(trip, "rice", 300.0)

        val line = shopping.stepsOf(trip).single()
        assertEquals(500.0, line.needed_amount)
        assertEquals(200.0, line.have_amount, "what the wizard showed when the call was made")
        assertEquals(300.0, line.suggested_amount)
    }

    @Test
    fun reenteringAnAmountAppendsTheDifference() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)

        shopping.buyStep(trip, "rice", 800.0)
        shopping.buyStep(trip, "rice", 500.0)

        assertEquals(500.0, pantry.amountOf("rice"), "stock matches the corrected amount")
        assertEquals(1, shopping.stepsOf(trip).size, "one line, corrected")
        assertEquals(listOf(800.0, -300.0), pantry.movementsOf("rice").map { it.delta }.reversed())
    }

    @Test
    fun skippingAddsNoStock() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)

        shopping.skipStep(trip, "rice")

        assertEquals(0.0, pantry.amountOf("rice"))
        assertEquals(clock.now, shopping.stepsOf(trip).single().skipped_at)
        assertTrue(pantry.movementsOf("rice").isEmpty())
    }

    @Test
    fun completingStampsTheTrip() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)

        shopping.complete(trip)

        val row = assertNotNull(db.shoppingTripQueries.findById(trip).executeAsOneOrNull())
        assertEquals(SessionStatus.DONE, row.status)
        assertEquals(clock.now, row.completed_at)
        assertNull(shopping.open(), "and it is no longer the open trip")
    }

    @Test
    fun askingTwiceContinuesTheSameTrip() = runTest {
        val midweek = blocks()
        planWithFridayRice()

        val first = shopping.forBlock(midweek, thursday)
        val second = shopping.forBlock(midweek, thursday)

        assertEquals(first, second, "one trip, continued")
    }

    @Test
    fun aForgottenTripWithNothingBoughtIsAbandoned() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val forgotten = shopping.forBlock(midweek, thursday)

        val weekend = prepBlocks.list().single { it.name == "Weekend" }.id
        val next = shopping.forBlock(weekend, thursday)

        assertEquals(
            SessionStatus.ABANDONED,
            db.shoppingTripQueries.findById(forgotten).executeAsOne().status,
            "nothing happened on it",
        )
        assertEquals(next, shopping.open()?.id)
        assertEquals(1, shopping.committedWindows(thursday).size, "only the new window is committed")
    }

    @Test
    fun aForgottenTripWithPurchasesIsCompleted() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val forgotten = shopping.forBlock(midweek, thursday)
        shopping.buyStep(forgotten, "rice", 500.0)

        val weekend = prepBlocks.list().single { it.name == "Weekend" }.id
        shopping.forBlock(weekend, thursday)

        assertEquals(
            SessionStatus.DONE,
            db.shoppingTripQueries.findById(forgotten).executeAsOne().status,
            "the food is in the pantry, so it happened",
        )
        assertEquals(500.0, pantry.amountOf("rice"), "and stays there")
        assertEquals(2, shopping.committedWindows(thursday).size, "a shopped window stays committed")
    }

    @Test
    fun anAbandonedTripCommitsNothing() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)
        assertEquals(1, shopping.committedWindows(thursday).size)

        shopping.abandon(trip)

        assertTrue(shopping.committedWindows(thursday).isEmpty())
    }

    @Test
    fun aTripCommitsItsWindowSoThePlanMovesToTheNextOne() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        shopping.forBlock(midweek, thursday)

        val activation = earliestPlanActivation(thursday, prepBlocks.list(), shopping.committedWindows(thursday))

        assertEquals(Slot(sunday, 1), activation, "the Midweek window is shopped for; edits start at Weekend")
    }

    @Test
    fun skippingSomethingAlreadyEnteredTakesItBackOut() = runTest {
        val midweek = blocks()
        planWithFridayRice()
        val trip = shopping.forBlock(midweek, thursday)
        shopping.buyStep(trip, "rice", 800.0)

        shopping.skipStep(trip, "rice")

        assertEquals(0.0, pantry.amountOf("rice"), "it never entered the pantry")
        assertEquals(listOf(800.0, -800.0), pantry.movementsOf("rice").map { it.delta }.reversed())
        val line = shopping.stepsOf(trip).single()
        assertNull(line.bought_amount)
        assertEquals(clock.now, line.skipped_at)
    }
}
