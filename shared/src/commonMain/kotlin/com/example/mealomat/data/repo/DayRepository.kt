package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.Day
import com.example.mealomat.domain.PlanVersion
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.cookDeduction
import com.example.mealomat.domain.projectDay
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive

class DayRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
    private val plan: PlanRepository,
    private val pantry: PantryRepository,
) {
    private val dayItemQueries = db.dayItemQueries
    private val planItemQueries = db.planItemQueries

    fun byDate(date: LocalDate): Day? {
        val version = plan.activeAt(Slot(date, 0)) ?: return null
        return projectDay(
            date = date,
            weekday = date.dayOfWeek,
            plan = PlanVersion(plan.mealsOf(version.id), plan.componentsOf(version.id), plan.itemsOf(version.id)),
            state = dayItemQueries.listForDate(date.toString()).executeAsList(),
        )
    }

    suspend fun tickOff(date: LocalDate, planItemId: String) =
        writeItem(date, planItemId) { row, now -> row.copy(ticked_at = now) }

    suspend fun markPrepped(date: LocalDate, planItemId: String) =
        writeItem(date, planItemId) { row, now -> row.copy(prepped_at = now) }

    suspend fun setExcluded(date: LocalDate, planItemId: String, excluded: Boolean) =
        writeItem(date, planItemId) { row, _ -> row.copy(excluded = excluded) }
    
    private suspend fun writeItem(
        date: LocalDate,
        planItemId: String,
        change: (Day_item, Long) -> Day_item,
    ): String {
        val userId = auth.requireUserId()
        val now = clock.nowMillis()
        val previous = findItem(date, planItemId)
        val row = change(previous ?: newRow(date, planItemId, userId, now), now)
            .copy(updated_at = now)
        val move = moveFor(previous, row, now)

        db.transaction {
            db.writeWithOutbox(outbox, Tables.DAY_ITEM, row.id, OutboxOp.UPSERT) {
                dayItemQueries.upsert(row)
                row.toPayload()
            }
            if (move != null) pantry.record(move, userId = userId, now = now)
        }
        return row.id
    }

    private fun moveFor(previous: Day_item?, row: Day_item, now: Long): PantryMove? = when {
        previous?.ticked_at == null && row.ticked_at != null -> cookMove(previous, row, now)
        else -> null
    }

    private fun cookMove(previous: Day_item?, row: Day_item, now: Long): PantryMove? {
        val planItem = planItemQueries.findById(row.plan_item_id).executeAsOneOrNull() ?: return null
        val use = cookDeduction(previous, planItem) ?: return null
        return PantryMove(
            ingredientId = use.ingredientId,
            delta = -use.amount,
            reason = LedgerReason.COOK,
            sourceKind = LedgerSource.DAY_ITEM,
            sourceId = row.id,
            occurredAt = now,
        )
    }

    private fun findItem(date: LocalDate, planItemId: String) =
        dayItemQueries.findByDateAndPlanItem(date.toString(), planItemId).executeAsOneOrNull()
}

private fun newRow(date: LocalDate, planItemId: String, userId: String, now: Long) = Day_item(
    id = newId(null),
    user_id = userId,
    plan_item_id = planItemId,
    updated_at = now,
    deleted_at = null,
    prepped_at = null,
    ticked_at = null,
    date = date.toString(),
    excluded = false,
)

private fun Day_item.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_item_id" to JsonPrimitive(plan_item_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "prepped_at" to JsonPrimitive(prepped_at),
    "ticked_at" to JsonPrimitive(ticked_at),
    "date" to JsonPrimitive(date),
    "excluded" to JsonPrimitive(excluded),
)
