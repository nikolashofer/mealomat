package com.example.mealomat.data.repo

import app.cash.sqldelight.db.SqlDriver
import com.example.mealomat.data.db.MealomatDatabase
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: MealomatDatabase
    private lateinit var repo: PlanRepository
    private val clock = FixedClock()

    private val monday = LocalDate(2026, 6, 22)
    private val sunday = LocalDate(2026, 6, 28)

    @BeforeTest
    fun setUp() {
        driver = testDriver()
        db = mealomatDatabase(driver)
        repo = PlanRepository(db, OutboxWriter(db, clock), FakeAuth("user-1"), clock)
    }

    private suspend fun seededPlan(): String {
        val plan = repo.create(Slot(monday, 0))
        val meal = repo.upsertMeal(plan, PlanMealDraft(weekday = DayOfWeek.MONDAY, name = "Lunch", position = 0))
        val component = repo.upsertComponent(
            plan, PlanComponentDraft(planMealId = meal, name = "Bowl", position = 0, prepMode = PrepMode.PREP),
        )
        repo.upsertItem(plan, PlanItemDraft(planMealId = meal, planComponentId = component, ingredientId = "rice", amount = 180.0, position = 0))
        repo.upsertItem(plan, PlanItemDraft(planMealId = meal, ingredientId = "egg", amount = 2.0, position = 1))
        return plan
    }

    @Test
    fun everyTableWritesAnOutboxEntry() = runTest {
        seededPlan()

        val tables = db.syncOutboxQueries.list().executeAsList().map { it.table_name }
        assertTrue(tables.containsAll(listOf(Tables.PLAN, Tables.PLAN_MEAL, Tables.PLAN_COMPONENT, Tables.PLAN_ITEM)))
    }

    @Test
    fun itemPrepModeStaysNullWhenNotOverridden() = runTest {
        val plan = seededPlan()

        val items = repo.itemsOf(plan)
        assertNull(items.single { it.ingredient_id == "egg" }.prep_mode, "NULL means inherit")
        assertNull(items.single { it.ingredient_id == "rice" }.prep_mode)
    }

    @Test
    fun weekdayRoundTripsAsTheIsoDayNumber() = runTest {
        val plan = repo.create(Slot(monday, 0))
        repo.upsertMeal(plan, PlanMealDraft(weekday = DayOfWeek.SUNDAY, name = "Brunch", position = 0))

        assertEquals(DayOfWeek.SUNDAY, repo.mealsOf(plan).single().weekday)
        val payload = db.syncOutboxQueries.list().executeAsList()
            .single { it.table_name == Tables.PLAN_MEAL }.payload
        assertEquals(7, Json.decodeFromString(JsonObject.serializer(), payload).getValue("weekday").jsonPrimitive.int)
    }

    @Test
    fun editingCopiesEveryRowWithFreshIds() = runTest {
        val v1 = seededPlan()

        val v2 = repo.forEditing(monday, Slot(sunday, 1))

        assertTrue(v2 != v1)
        assertEquals(repo.mealsOf(v1).size, repo.mealsOf(v2).size)
        assertEquals(repo.itemsOf(v1).size, repo.itemsOf(v2).size)
        assertTrue(repo.itemsOf(v1).map { it.id }.intersect(repo.itemsOf(v2).map { it.id }.toSet()).isEmpty())
        assertEquals(v2, repo.itemsOf(v2).first().plan_id, "children point at the new version")
    }

    @Test
    fun componentsKeepTheirLineageAcrossVersions() = runTest {
        val v1 = seededPlan()

        val v2 = repo.forEditing(monday, Slot(sunday, 1))

        val before = repo.componentsOf(v1).single()
        val after = repo.componentsOf(v2).single()
        assertTrue(before.id != after.id, "a new row")
        assertEquals(before.lineage_id, after.lineage_id, "but the same lineage")
    }

    @Test
    fun copiedChildrenPointAtTheCopiedParents() = runTest {
        val v1 = seededPlan()
        val v2 = repo.forEditing(monday, Slot(sunday, 1))

        val mealIds = repo.mealsOf(v2).map { it.id }.toSet()
        val componentIds = repo.componentsOf(v2).map { it.id }.toSet()
        assertTrue(repo.itemsOf(v2).all { it.plan_meal_id in mealIds })
        assertTrue(repo.itemsOf(v2).mapNotNull { it.plan_component_id }.all { it in componentIds })
        assertTrue(repo.itemsOf(v1).none { it.plan_meal_id in mealIds }, "v1 is untouched")
    }

    @Test
    fun editingReusesTheScheduledVersion() = runTest {
        seededPlan()

        val first = repo.forEditing(monday, Slot(sunday, 1))
        val second = repo.forEditing(monday, Slot(sunday, 1))

        assertEquals(first, second)
        assertEquals(2, repo.list().size, "one revision, not two")
    }

    @Test
    fun aStaleScheduledVersionIsNotReused() = runTest {
        seededPlan()
        val stale = repo.forEditing(monday, Slot(sunday, 1))
        val staleActiveFrom = repo.list().single { it.id == stale }.active_from_date

        val fresh = repo.forEditing(monday, Slot(LocalDate(2026, 7, 2), 0))

        assertTrue(fresh != stale)
        assertEquals(3, repo.list().size)
        assertEquals(staleActiveFrom, repo.list().single { it.id == stale }.active_from_date)
    }

    @Test
    fun planAtPicksTheVersionOwningTheDate() = runTest {
        val v1 = seededPlan()
        val v2 = repo.forEditing(monday, Slot(sunday, 1))

        assertEquals(v1, repo.activeAt(Slot(LocalDate(2026, 6, 24), 0))?.id)
        assertEquals(v1, repo.activeAt(Slot(sunday, 0))?.id, "Sunday breakfast is still v1")
        assertEquals(v2, repo.activeAt(Slot(sunday, 1))?.id, "Sunday lunch onwards is v2")
    }

    @Test
    fun twoVersionsCannotStartAtTheSameSlot() = runTest {
        repo.create(Slot(monday, 0))

        assertFails { repo.create(Slot(monday, 0)) }
    }

    @Test
    fun versionsMayStartAtDifferentPositionsOnTheSameDay() = runTest {
        repo.create(Slot(sunday, 0))
        repo.create(Slot(sunday, 1))

        assertEquals(2, repo.list().size, "Sunday splits between two versions")
    }

    @Test
    fun isEmptyReportsWhetherAPlanExists() = runTest {
        assertTrue(repo.isEmpty())
        repo.create(Slot(monday, 0))
        assertTrue(!repo.isEmpty())
    }
}
