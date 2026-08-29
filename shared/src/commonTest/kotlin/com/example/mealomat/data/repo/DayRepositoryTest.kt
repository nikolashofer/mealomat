package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.PrepMode
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DayRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var plan: PlanRepository
    private lateinit var days: DayRepository
    private lateinit var pantry: PantryRepository
    private val clock = FixedClock()

    private val monday = LocalDate(2026, 6, 22) // v1 starts here
    private val nextMonday = LocalDate(2026, 6, 29)

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        val outbox = OutboxWriter(db, clock)
        val auth = FakeAuth("user-1")
        plan = PlanRepository(db, outbox, auth, clock)
        pantry = PantryRepository(db, outbox, auth, clock)
        days = DayRepository(db, outbox, auth, clock, plan, pantry)
    }

    private suspend fun mondayLunch(planId: String, amount: Double = 180.0): String {
        val meal = plan.upsertMeal(planId, PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))
        return plan.upsertItem(planId, PlanItemDraft(planMealId = meal, ingredientId = "rice", amount = amount, position = 0))
    }

    private fun itemOn(date: LocalDate) = days.byDate(date)!!.meals.single().items.single()

    @Test
    fun aDayProjectsThePlanVersionThatOwnsIt() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        mondayLunch(v1, amount = 180.0)

        assertEquals(180.0, itemOn(monday).amount)
        assertEquals(PrepMode.FRESH, itemOn(monday).prepMode, "a flat item with no prep mode is FRESH")
    }

    @Test
    fun aDateBeforeEveryVersionHasNoDay() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        mondayLunch(v1)

        assertNull(days.byDate(LocalDate(2026, 6, 15)))
    }

    @Test
    fun aPastDayKeepsTheAmountItWasEatenAt() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1, amount = 180.0)
        days.tickOff(monday, item)

        // the plan changes from next Monday on
        val v2 = plan.forEditing(monday, Slot(nextMonday, 0))
        val copied = plan.itemsOf(v2).single()
        plan.upsertItem(v2, PlanItemDraft(id = copied.id, planMealId = copied.plan_meal_id, ingredientId = "rice", amount = 200.0, position = 0))

        assertEquals(180.0, itemOn(monday).amount, "history is exact")
        assertEquals(200.0, itemOn(nextMonday).amount, "the future follows the new version")
    }

    @Test
    fun stateSurvivesALaterRevision() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)
        days.tickOff(monday, item)

        plan.forEditing(monday, Slot(nextMonday, 0))

        assertTrue(itemOn(monday).tickedAt != null, "the tick is still attached")
        assertNull(itemOn(nextMonday).tickedAt, "and did not leak to the new version")
    }

    @Test
    fun aDayNobodyTouchedHasNoStateRows() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        mondayLunch(v1)

        assertEquals(0, db.dayItemQueries.listForDate(monday.toString()).executeAsList().size)
        assertEquals(1, days.byDate(monday)!!.meals.single().items.size, "but the day still renders")
    }

    @Test
    fun repeatedTouchesUpdateOneRow() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)

        days.setExcluded(monday, item, excluded = true)
        days.markPrepped(monday, item)
        days.tickOff(monday, item)

        val rows = db.dayItemQueries.listForDate(monday.toString()).executeAsList()
        assertEquals(1, rows.size)
        assertTrue(rows.single().excluded && rows.single().prepped_at != null)
        assertTrue(rows.single().ticked_at != null)
    }

    @Test
    fun everyStateChangeIsQueuedForSync() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)
        val before = db.syncOutboxQueries.count().executeAsOne()

        days.tickOff(monday, item)

        val tables = db.syncOutboxQueries.list().executeAsList().map { it.table_name }
        assertTrue(db.syncOutboxQueries.count().executeAsOne() > before)
        assertTrue(tables.contains(Tables.DAY_ITEM))
    }

    @Test
    fun aPlannedEatOutDropsOutOfTheDay() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)

        days.setExcluded(monday, item, true)

        assertTrue(itemOn(monday).excluded)
        assertTrue(itemOn(monday).isDone, "an excluded item is nothing to do")
    }

    @Test
    fun aMealIsDoneWhenEveryLineIsReadyOrTicked() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val meal = plan.upsertMeal(v1, PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))
        val rice = plan.upsertItem(v1, PlanItemDraft(planMealId = meal, ingredientId = "rice", amount = 180.0, position = 0))
        val egg = plan.upsertItem(v1, PlanItemDraft(planMealId = meal, ingredientId = "egg", amount = 2.0, position = 1))

        assertTrue(!days.byDate(monday)!!.meals.single().isDone)
        days.markPrepped(monday, rice)
        assertTrue(!days.byDate(monday)!!.meals.single().isDone, "one item still to do")
        days.tickOff(monday, egg)
        assertTrue(days.byDate(monday)!!.meals.single().isDone)
    }

    @Test
    fun aMealWithNoLinesIsNotDone() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        plan.upsertMeal(v1, PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))

        assertTrue(!days.byDate(monday)!!.meals.single().isDone)
    }

    @Test
    fun anUneatenMealNeedsNoStoredStatus() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        mondayLunch(v1)

        assertTrue(!days.byDate(monday)!!.meals.single().isDone)
        assertEquals(0, db.dayItemQueries.listForDate(monday.toString()).executeAsList().size)
    }

    @Test
    fun itemsUnderAComponentInheritItsPrepMode() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val meal = plan.upsertMeal(v1, PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))
        val component = plan.upsertComponent(v1, PlanComponentDraft(planMealId = meal, name = "Bowl", position = 0, prepMode = PrepMode.PREP))
        plan.upsertItem(v1, PlanItemDraft(planMealId = meal, planComponentId = component, ingredientId = "rice", amount = 180.0, position = 0))
        plan.upsertItem(v1, PlanItemDraft(planMealId = meal, ingredientId = "egg", amount = 2.0, position = 1))

        val items = days.byDate(monday)!!.meals.single().items.associateBy { it.ingredientId }
        assertEquals(PrepMode.PREP, items.getValue("rice").prepMode, "inherited")
        assertEquals(PrepMode.FRESH, items.getValue("egg").prepMode, "flat item defaults to FRESH")
    }

    @Test
    fun tickingOffDeductsThePlannedAmountOnce() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1, amount = 180.0)

        days.tickOff(monday, item)
        days.tickOff(monday, item)

        assertEquals(-180.0, pantry.amountOf("rice"), "the second tap moves nothing")
        val ledger = pantry.movementsOf("rice").single()
        assertEquals(LedgerReason.COOK, ledger.reason)
        assertEquals(LedgerSource.DAY_ITEM, ledger.source_kind)
        assertEquals(
            db.dayItemQueries.listForDate(monday.toString()).executeAsList().single().id,
            ledger.source_id,
            "the ledger points back at the line that caused it",
        )
    }

    @Test
    fun tickingAPreppedItemDeductsNothing() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)

        days.markPrepped(monday, item)
        days.tickOff(monday, item)

        assertEquals(0.0, pantry.amountOf("rice"), "it already left the pantry at prep time")
        assertTrue(pantry.movementsOf("rice").isEmpty())
    }

    @Test
    fun tickingAnExcludedItemDeductsNothing() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)

        days.setExcluded(monday, item, excluded = true)
        days.tickOff(monday, item)

        assertEquals(0.0, pantry.amountOf("rice"), "a planned eat-out was never bought")
    }

    @Test
    fun aSignedOutTickWritesNothing() = runTest {
        val v1 = plan.create(Slot(monday, 0))
        val item = mondayLunch(v1)
        val signedOut = DayRepository(db, OutboxWriter(db, clock), FakeAuth(null), clock, plan, pantry)

        assertFailsWith<IllegalArgumentException> { signedOut.tickOff(monday, item) }
        assertNull(itemOn(monday).tickedAt)
        assertTrue(pantry.movementsOf("rice").isEmpty())
    }
}
