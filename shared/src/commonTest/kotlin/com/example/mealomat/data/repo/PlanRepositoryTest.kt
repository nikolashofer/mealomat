package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.PrepMode
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.testing.FakeAuth
import com.example.mealomat.testing.FixedClock
import com.example.mealomat.testing.testDriver
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var repo: PlanRepository
    private val clock = FixedClock()

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        repo = PlanRepository(db, OutboxWriter(db, clock), FakeAuth("user-1"), clock)
    }

    private suspend fun mondayLunch(): String =
        repo.upsertMeal(PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))

    @Test
    fun everyTableWritesAnOutboxEntry() = runTest {
        val meal = mondayLunch()
        val component = repo.upsertComponent(
            PlanComponentDraft(planMealId = meal, name = "Peanut sauce", position = 0, prepMode = PrepMode.PREP),
        )
        repo.upsertItem(
            PlanItemDraft(
                planMealId = meal,
                planComponentId = component,
                ingredientId = "ing-1",
                amount = 40.0,
                position = 0,
            ),
        )

        assertEquals(3L, db.syncOutboxQueries.count().executeAsOne())
        val tables = listOf(Tables.PLAN_MEAL, Tables.PLAN_COMPONENT, Tables.PLAN_ITEM)
        tables.forEach { table ->
            assertTrue(
                db.syncOutboxQueries.list().executeAsList().any { it.table_name == table },
                "no outbox entry for $table",
            )
        }
    }

    @Test
    fun itemPrepModeStaysNullWhenNotOverridden() = runTest {
        val meal = mondayLunch()
        repo.upsertItem(PlanItemDraft(planMealId = meal, ingredientId = "ing-1", amount = 80.0, position = 0))
        repo.upsertItem(
            PlanItemDraft(
                planMealId = meal,
                ingredientId = "ing-2",
                amount = 2.0,
                position = 1,
                prepMode = PrepMode.FRESH,
            ),
        )

        val items = repo.items()
        assertNull(items.single { it.ingredient_id == "ing-1" }.prep_mode)
        assertEquals(PrepMode.FRESH, items.single { it.ingredient_id == "ing-2" }.prep_mode)
    }

    @Test
    fun itemsCanBeFlatOrUnderAComponent() = runTest {
        val meal = mondayLunch()
        val component = repo.upsertComponent(
            PlanComponentDraft(planMealId = meal, name = "Bowl base", position = 0, prepMode = PrepMode.PREP),
        )
        repo.upsertItem(PlanItemDraft(planMealId = meal, planComponentId = component, ingredientId = "rice", amount = 180.0, position = 0))
        repo.upsertItem(PlanItemDraft(planMealId = meal, ingredientId = "egg", amount = 2.0, position = 1))

        val items = repo.items()
        assertEquals(component, items.single { it.ingredient_id == "rice" }.plan_component_id)
        assertNull(items.single { it.ingredient_id == "egg" }.plan_component_id)
    }

    @Test
    fun payloadKeysAreColumnNames() = runTest {
        val meal = mondayLunch()
        repo.upsertItem(PlanItemDraft(planMealId = meal, ingredientId = "ing-1", amount = 80.0, position = 0))

        val payload = db.syncOutboxQueries.list().executeAsList()
            .single { it.table_name == Tables.PLAN_ITEM }.payload
        val json = Json.decodeFromString(JsonObject.serializer(), payload)
        assertTrue(
            json.keys.containsAll(listOf("plan_meal_id", "plan_component_id", "ingredient_id", "prep_mode")),
        )
        assertTrue(json.keys.none { it.any(Char::isUpperCase) }, "camelCase leaked: ${json.keys}")
        assertEquals("user-1", json.getValue("user_id").jsonPrimitive.content)
    }

    @Test
    fun isEmptyReportsWhetherAPlanExists() = runTest {
        assertTrue(repo.isEmpty())
        mondayLunch()
        assertTrue(!repo.isEmpty())
    }

    @Test
    fun weekdayRoundTripsAsTheIsoDayNumber() = runTest {
        repo.upsertMeal(PlanMealDraft(weekday = DayOfWeek.SUNDAY, name = "Brunch", position = 0))

        assertEquals(DayOfWeek.SUNDAY, repo.meals().single().weekday)
        val payload = db.syncOutboxQueries.list().executeAsList()
            .single { it.table_name == Tables.PLAN_MEAL }.payload
        assertEquals(
            7,
            Json.decodeFromString(JsonObject.serializer(), payload).getValue("weekday").jsonPrimitive.int,
        )
    }

    @Test
    fun mealsAreOrderedByWeekdayThenPosition() = runTest {
        repo.upsertMeal(PlanMealDraft(weekday = DayOfWeek.TUESDAY, name = "Lunch", position = 1))
        repo.upsertMeal(PlanMealDraft(weekday = DayOfWeek.TUESDAY, name = "Breakfast", position = 0))
        repo.upsertMeal(PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Dinner", position = 0))

        assertEquals(listOf("Dinner", "Breakfast", "Lunch"), repo.meals().map { it.name })
    }
}
