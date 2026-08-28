package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Plan
import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Plan_item
import com.example.mealomat.data.db.Plan_meal
import com.example.mealomat.data.db.PrepMode
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.activeFrom
import com.example.mealomat.domain.planFor
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.json.JsonPrimitive

data class PlanMealDraft(
    val id: String? = null,
    val weekday: DayOfWeek,
    val name: String,
    val position: Int,
)

data class PlanComponentDraft(
    val id: String? = null,
    val lineageId: String? = null,
    val planMealId: String,
    val name: String,
    val position: Int,
    val prepMode: PrepMode,
)

data class PlanItemDraft(
    val id: String? = null,
    val planMealId: String,
    val planComponentId: String? = null,
    val ingredientId: String,
    val amount: Double,
    val position: Int,
    val prepMode: PrepMode? = null,
)

class PlanRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
) {
    private val planQueries = db.planQueries
    private val mealQueries = db.planMealQueries
    private val componentQueries = db.planComponentQueries
    private val itemQueries = db.planItemQueries

    fun plans(): List<Plan> = planQueries.list().executeAsList()

    fun planAt(slot: Slot): Plan? = planFor(slot, plans())

    fun meals(planId: String): List<Plan_meal> = mealQueries.listForPlan(planId).executeAsList()

    fun components(planId: String): List<Plan_component> =
        componentQueries.listForPlan(planId).executeAsList()

    fun items(planId: String): List<Plan_item> = itemQueries.listForPlan(planId).executeAsList()

    fun isEmpty(): Boolean = plans().isEmpty()

    suspend fun createPlan(activeFrom: Slot): String {
        val row = Plan(
            id = newId(null), user_id = auth.requireUserId(),
            updated_at = clock.nowMillis(), deleted_at = null,
            active_from_date = activeFrom.date.toString(),
            active_from_position = activeFrom.position.toLong(),
        )
        db.writeWithOutbox(outbox, Tables.PLAN, row.id, OutboxOp.UPSERT) {
            planQueries.insert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun editablePlan(now: LocalDate, earliest: Slot): String {
        val scheduled = plans().lastOrNull { it.activeFrom() > Slot(now, Int.MAX_VALUE) }
        if (scheduled != null && scheduled.activeFrom() >= earliest) return scheduled.id

        val source = planFor(Slot(now, 0), plans())
        val copy = createPlan(earliest)
        if (source != null) copyInto(copy, source.id)
        return copy
    }

    suspend fun upsertMeal(planId: String, draft: PlanMealDraft): String {
        val row = draft.toRow(newId(draft.id), planId, auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_MEAL, row.id, OutboxOp.UPSERT) {
            mealQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun upsertComponent(planId: String, draft: PlanComponentDraft): String {
        val id = newId(draft.id)
        val row = draft.toRow(id, draft.lineageId ?: id, planId, auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_COMPONENT, row.id, OutboxOp.UPSERT) {
            componentQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun upsertItem(planId: String, draft: PlanItemDraft): String {
        val row = draft.toRow(newId(draft.id), planId, auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_ITEM, row.id, OutboxOp.UPSERT) {
            itemQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun softDeleteItem(id: String) {
        val now = clock.nowMillis()
        db.writeWithOutbox(outbox, Tables.PLAN_ITEM, id, OutboxOp.DELETE) {
            itemQueries.softDelete(deleted_at = now, updated_at = now, id = id)
            itemQueries.findById(id).executeAsOneOrNull()?.toPayload()
        }
    }

    private suspend fun copyInto(planId: String, sourceId: String) {
        val userId = auth.requireUserId()
        val now = clock.nowMillis()
        db.transaction { copyRows(planId, sourceId, userId, now) }
    }

    private fun copyRows(planId: String, sourceId: String, userId: String, now: Long) {
        val mealIds = mutableMapOf<String, String>()
        val componentIds = mutableMapOf<String, String>()

        meals(sourceId).forEach { source ->
            val row = source.copy(id = newId(null), plan_id = planId, user_id = userId, updated_at = now)
            mealIds[source.id] = row.id
            db.writeWithOutbox(outbox, Tables.PLAN_MEAL, row.id, OutboxOp.UPSERT) {
                mealQueries.upsert(row)
                row.toPayload()
            }
        }
        components(sourceId).forEach { source ->
            val row = source.copy(
                id = newId(null), plan_id = planId, user_id = userId, updated_at = now,
                plan_meal_id = mealIds.getValue(source.plan_meal_id),
            )
            componentIds[source.id] = row.id
            db.writeWithOutbox(outbox, Tables.PLAN_COMPONENT, row.id, OutboxOp.UPSERT) {
                componentQueries.upsert(row)
                row.toPayload()
            }
        }
        items(sourceId).forEach { source ->
            val row = source.copy(
                id = newId(null), plan_id = planId, user_id = userId, updated_at = now,
                plan_meal_id = mealIds.getValue(source.plan_meal_id),
                plan_component_id = source.plan_component_id?.let(componentIds::getValue),
            )
            db.writeWithOutbox(outbox, Tables.PLAN_ITEM, row.id, OutboxOp.UPSERT) {
                itemQueries.upsert(row)
                row.toPayload()
            }
        }
    }
}

private fun PlanMealDraft.toRow(id: String, planId: String, userId: String, now: Long) = Plan_meal(
    id = id,
    user_id = userId,
    plan_id = planId,
    updated_at = now,
    deleted_at = null,
    weekday = weekday,
    name = name,
    position = position.toLong(),
)

private fun PlanComponentDraft.toRow(
    id: String, lineageId: String, planId: String, userId: String, now: Long,
) = Plan_component(
    id = id,
    user_id = userId,
    plan_id = planId,
    plan_meal_id = planMealId,
    lineage_id = lineageId,
    updated_at = now,
    deleted_at = null,
    name = name,
    position = position.toLong(),
    prep_mode = prepMode,
)

private fun PlanItemDraft.toRow(id: String, planId: String, userId: String, now: Long) = Plan_item(
    id = id,
    user_id = userId,
    plan_id = planId,
    plan_meal_id = planMealId,
    plan_component_id = planComponentId,
    ingredient_id = ingredientId,
    updated_at = now,
    deleted_at = null,
    amount = amount,
    position = position.toLong(),
    prep_mode = prepMode,
)

private fun Plan.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "active_from_date" to JsonPrimitive(active_from_date),
    "active_from_position" to JsonPrimitive(active_from_position),
)

private fun Plan_meal.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_id" to JsonPrimitive(plan_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "weekday" to JsonPrimitive(weekday.isoDayNumber),
    "name" to JsonPrimitive(name),
    "position" to JsonPrimitive(position),
)

private fun Plan_component.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_id" to JsonPrimitive(plan_id),
    "plan_meal_id" to JsonPrimitive(plan_meal_id),
    "lineage_id" to JsonPrimitive(lineage_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "name" to JsonPrimitive(name),
    "position" to JsonPrimitive(position),
    "prep_mode" to JsonPrimitive(prep_mode.name),
)

private fun Plan_item.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_id" to JsonPrimitive(plan_id),
    "plan_meal_id" to JsonPrimitive(plan_meal_id),
    "plan_component_id" to JsonPrimitive(plan_component_id),
    "ingredient_id" to JsonPrimitive(ingredient_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "amount" to JsonPrimitive(amount),
    "position" to JsonPrimitive(position),
    "prep_mode" to JsonPrimitive(prep_mode?.name),
)
