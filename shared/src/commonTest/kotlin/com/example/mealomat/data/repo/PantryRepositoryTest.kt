package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PantryRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var pantry: PantryRepository
    private val clock = FixedClock()

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        pantry = PantryRepository(db, OutboxWriter(db, clock), FakeAuth("user-1"), clock)
    }

    @Test
    fun recordingAppendsARowAndMovesStock() = runTest {
        val id = pantry.record(PantryMove("rice", 500.0, LedgerReason.BUY))

        assertEquals(500.0, pantry.amountOf("rice"))
        val row = pantry.movementsOf("rice").single()
        assertEquals(id, row.id)
        assertEquals("user-1", row.user_id)
        assertEquals(clock.now, row.occurred_at)
        assertEquals(LedgerSource.MANUAL, row.source_kind)

        val entries = db.syncOutboxQueries.listForRow(Tables.PANTRY_LEDGER, id).executeAsList()
        assertEquals(1, entries.size)
        assertEquals("UPSERT", entries.single().op)
    }

    @Test
    fun stockIsTheSumOfItsMovements() = runTest {
        pantry.addAhead("rice", 500.0)
        pantry.consume("rice", 120.0)
        pantry.spoil("rice", 30.0)

        assertEquals(350.0, pantry.amountOf("rice"))
        assertEquals(3, pantry.movementsOf("rice").size)
    }

    @Test
    fun stockIsAllowedToGoNegative() = runTest {
        pantry.consume("rice", 80.0)

        assertEquals(-80.0, pantry.amountOf("rice"), "a tick-off is never blocked by empty stock")
    }

    @Test
    fun adjustAppendsTheDifferenceAndKeepsTheHistory() = runTest {
        pantry.addAhead("rice", 500.0)
        pantry.adjustTo("rice", 420.0, note = "weighed it")

        assertEquals(420.0, pantry.amountOf("rice"))
        val rows = pantry.movementsOf("rice")
        assertEquals(2, rows.size, "the original row stays; the correction is an append")
        val adjust = rows.first { it.reason == LedgerReason.ADJUST }
        assertEquals(-80.0, adjust.delta)
        assertEquals("weighed it", adjust.note)
    }

    @Test
    fun eachManualActionKeepsItsOwnReason() = runTest {
        pantry.consume("rice", 10.0)
        pantry.spoil("rice", 10.0)
        pantry.addAhead("rice", 10.0)

        val reasons = pantry.movementsOf("rice").map { it.reason }.toSet()
        assertEquals(setOf(LedgerReason.CONSUME, LedgerReason.SPOILAGE, LedgerReason.BUY), reasons)
    }

    @Test
    fun rebuildingStockAgreesWithTheIncrementalSum() = runTest {
        pantry.addAhead("rice", 500.0)
        pantry.consume("rice", 120.0)
        pantry.addAhead("oats", 1000.0)
        pantry.spoil("oats", 250.0)
        pantry.adjustTo("rice", 400.0)

        val incremental = listOf("rice", "oats").associateWith { pantry.amountOf(it) }
        pantry.rebuildStock()

        assertEquals(incremental, listOf("rice", "oats").associateWith { pantry.amountOf(it) })
        assertEquals(400.0, incremental.getValue("rice"), "the adjust set it outright")
        assertEquals(750.0, incremental.getValue("oats"))
    }

    @Test
    fun anIngredientWithNoMovementsHasNoStockRow() = runTest {
        assertEquals(0.0, pantry.amountOf("rice"))
        assertTrue(pantry.movementsOf("rice").isEmpty())
    }

    @Test
    fun writingWithoutASignedInUserFails() = runTest {
        val signedOut = PantryRepository(db, OutboxWriter(db, clock), FakeAuth(null), clock)
        assertFailsWith<IllegalArgumentException> {
            signedOut.record(PantryMove("rice", 1.0, LedgerReason.BUY))
        }
    }
}
