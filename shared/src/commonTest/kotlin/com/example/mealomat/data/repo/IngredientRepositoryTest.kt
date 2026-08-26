package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.DatabaseSessionScopedData
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngredientRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var repo: IngredientRepository
    private val clock = FixedClock()

    private fun draft(name: String = "Oats", archived: Boolean = false) = IngredientDraft(
        name = name, basis = Basis.G100, kcal = 370.0,
        proteinG = 13.0, carbsG = 59.0, fatG = 7.0, archived = archived,
    )

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        repo = IngredientRepository(db, OutboxWriter(db, clock), FakeAuth("user-1"), clock)
    }

    @Test
    fun upsertWritesRowAndOutboxEntry() = runTest {
        val id = repo.upsert(draft())

        val row = assertNotNull(repo.byId(id))
        assertEquals("user-1", row.user_id)
        assertEquals(clock.now, row.updated_at)
        assertNull(row.deleted_at)

        val entries = db.syncQueries.listPendingForRow("ingredient", id).executeAsList()
        assertEquals(1, entries.size)
        assertEquals("UPSERT", entries.single().op)
    }

    @Test
    fun softDeleteKeepsTheRowButHidesIt() = runTest {
        val id = repo.upsert(draft())
        clock.now += 1000
        repo.softDelete(id)

        val row = assertNotNull(repo.byId(id), "row must still physically exist")
        assertEquals(clock.now, row.deleted_at)
        assertTrue(db.ingredientQueries.listActive().executeAsList().none { it.id == id })
        assertEquals("DELETE", db.syncQueries.listPendingForRow("ingredient", id).executeAsList().last().op)
    }

    @Test
    fun archivedIsHiddenFromActiveButNotDeleted() = runTest {
        val id = repo.upsert(draft(archived = true))
        val row = assertNotNull(repo.byId(id))
        assertNull(row.deleted_at)
        assertTrue(row.archived)
        assertTrue(db.ingredientQueries.listActive().executeAsList().none { it.id == id })
        assertTrue(db.ingredientQueries.listWithArchived().executeAsList().any { it.id == id })
    }

    @Test
    fun outboxPayloadUsesColumnNames() = runTest {
        val id = repo.upsert(draft())
        val payload = db.syncQueries.listPendingForRow("ingredient", id).executeAsList().single().payload
        val keys = Json.decodeFromString(JsonObject.serializer(), payload).keys
        assertTrue(keys.containsAll(listOf("user_id", "updated_at", "deleted_at", "protein_g", "saturated_fat_g", "pack_size")))
        assertTrue(keys.none { it.any(Char::isUpperCase) }, "camelCase leaked into the payload: $keys")
    }

    @Test
    fun writingWithoutASignedInUserFails() = runTest {
        val signedOut = IngredientRepository(db, OutboxWriter(db, clock), FakeAuth(null), clock)
        assertFailsWith<IllegalArgumentException> { signedOut.upsert(draft()) }
    }

    @Test
    fun clearingLocalDataEmptiesEveryTable() = runTest {
        repo.upsert(draft())
        assertEquals(1, db.ingredientQueries.listWithArchived().executeAsList().size)
        assertEquals(1L, db.syncQueries.countPending().executeAsOne())

        DatabaseSessionScopedData(driver).clear()

        assertTrue(db.ingredientQueries.listWithArchived().executeAsList().isEmpty())
        assertEquals(0L, db.syncQueries.countPending().executeAsOne())
    }
}
