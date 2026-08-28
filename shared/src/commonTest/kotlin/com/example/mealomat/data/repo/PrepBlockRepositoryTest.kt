package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrepBlockRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var repo: PrepBlockRepository
    private val clock = FixedClock()

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        repo = PrepBlockRepository(db, OutboxWriter(db, clock), FakeAuth("user-1"), clock)
    }

    private suspend fun midweek() = repo.upsertBlock(
        PrepBlockDraft(
            name = "Midweek",
            prepWeekday = DayOfWeek.WEDNESDAY,
            shoppingWeekday = DayOfWeek.WEDNESDAY,
            coversFromWeekday = DayOfWeek.THURSDAY,
            coversFromPosition = 0,
        ),
    )

    private suspend fun weekend() = repo.upsertBlock(
        PrepBlockDraft(
            name = "Weekend",
            prepWeekday = DayOfWeek.SUNDAY,
            shoppingWeekday = DayOfWeek.SATURDAY,
            coversFromWeekday = DayOfWeek.SUNDAY,
            coversFromPosition = 1,
        ),
    )

    @Test
    fun blocksCarryTheWeekdaysTheNavDotsRead() = runTest {
        midweek()
        weekend()

        val blocks = repo.blocks()
        assertEquals(listOf("Midweek", "Weekend"), blocks.map { it.name })
        assertEquals(listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), blocks.map { it.prep_weekday })
        assertEquals(listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY), blocks.map { it.shopping_weekday })
    }

    @Test
    fun blocksCarryTheirCoverageBoundary() = runTest {
        weekend()

        val block = repo.blocks().single()
        assertEquals(DayOfWeek.SUNDAY, block.covers_from_weekday)
        assertEquals(1L, block.covers_from_position)
    }

    @Test
    fun overridesAreOrderedWithinTheirBlock() = runTest {
        val block = midweek()
        val other = weekend()
        repo.upsertOverride(PrepStepOverrideDraft(prepBlockId = block, targetKey = "ingredient:rice", position = 1))
        repo.upsertOverride(PrepStepOverrideDraft(prepBlockId = block, targetKey = "component:sauce", position = 0))
        repo.upsertOverride(PrepStepOverrideDraft(prepBlockId = other, targetKey = "ingredient:oats", position = 0))

        assertEquals(listOf("component:sauce", "ingredient:rice"), repo.overrides(block).map { it.target_key })
        assertEquals(listOf("ingredient:oats"), repo.overrides(other).map { it.target_key })
    }

    @Test
    fun everyTableWritesAnOutboxEntry() = runTest {
        val block = midweek()
        repo.upsertOverride(PrepStepOverrideDraft(prepBlockId = block, targetKey = "ingredient:rice", position = 0))

        val tables = db.syncOutboxQueries.list().executeAsList().map { it.table_name }
        assertTrue(tables.containsAll(listOf(Tables.PREP_BLOCK, Tables.PREP_STEP_OVERRIDE)))
    }

    @Test
    fun payloadKeysAreColumnNames() = runTest {
        weekend()

        val payload = db.syncOutboxQueries.list().executeAsList()
            .single { it.table_name == Tables.PREP_BLOCK }.payload
        val json = Json.decodeFromString(JsonObject.serializer(), payload)
        assertTrue(json.keys.containsAll(listOf("prep_weekday", "shopping_weekday", "covers_from_weekday", "covers_from_position")))
        assertEquals(7, json.getValue("covers_from_weekday").jsonPrimitive.int)
        assertEquals(6, json.getValue("shopping_weekday").jsonPrimitive.int)
    }
}
