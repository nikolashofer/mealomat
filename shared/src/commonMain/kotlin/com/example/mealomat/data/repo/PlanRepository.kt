package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Plan_component
import com.example.mealomat.data.db.Plan_item
import com.example.mealomat.data.db.Plan_meal
import com.example.mealomat.data.db.PrepMode
import kotlinx.datetime.DayOfWeek
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import kotlin.time.Clock
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
    private val mealQueries = db.planMealQueries
    private val componentQueries = db.planComponentQueries
    private val itemQueries = db.planItemQueries

    // TODO: add composite query, that returns meals with all its items/comps...

    fun meals(): List<Plan_meal> = mealQueries.list().executeAsList()

    fun components(): List<Plan_component> = componentQueries.list().executeAsList()

    fun items(): List<Plan_item> = itemQueries.list().executeAsList()

    fun isEmpty(): Boolean = mealQueries.count().executeAsOne() == 0L

    suspend fun upsertMeal(draft: PlanMealDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_MEAL, row.id, OutboxOp.UPSERT) {
            mealQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun upsertComponent(draft: PlanComponentDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_COMPONENT, row.id, OutboxOp.UPSERT) {
            componentQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun upsertItem(draft: PlanItemDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PLAN_ITEM, row.id, OutboxOp.UPSERT) {
            itemQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

}

private fun PlanMealDraft.toRow(id: String, userId: String, now: Long) = Plan_meal(
    id = id,
    user_id = userId,
    updated_at = now,
    deleted_at = null,
    weekday = weekday,
    name = name,
    position = position.toLong(),
)

private fun PlanComponentDraft.toRow(id: String, userId: String, now: Long) = Plan_component(
    id = id,
    user_id = userId,
    plan_meal_id = planMealId,
    updated_at = now,
    deleted_at = null,
    name = name,
    position = position.toLong(),
    prep_mode = prepMode,
)

private fun PlanItemDraft.toRow(id: String, userId: String, now: Long) = Plan_item(
    id = id,
    user_id = userId,
    plan_meal_id = planMealId,
    plan_component_id = planComponentId,
    ingredient_id = ingredientId,
    updated_at = now,
    deleted_at = null,
    amount = amount,
    position = position.toLong(),
    prep_mode = prepMode,
)

private fun Plan_meal.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "weekday" to JsonPrimitive(weekday.isoDayNumber),
    "name" to JsonPrimitive(name),
    "position" to JsonPrimitive(position),
)

private fun Plan_component.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_meal_id" to JsonPrimitive(plan_meal_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "name" to JsonPrimitive(name),
    "position" to JsonPrimitive(position),
    "prep_mode" to JsonPrimitive(prep_mode.name),
)

private fun Plan_item.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_meal_id" to JsonPrimitive(plan_meal_id),
    "plan_component_id" to JsonPrimitive(plan_component_id),
    "ingredient_id" to JsonPrimitive(ingredient_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "amount" to JsonPrimitive(amount),
    "position" to JsonPrimitive(position),
    "prep_mode" to JsonPrimitive(prep_mode?.name),
)
