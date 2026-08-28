package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.Day_item
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.Day
import com.example.mealomat.domain.PlanVersion
import com.example.mealomat.domain.Slot
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
) {
    private val itemQueries = db.dayItemQueries

    fun day(date: LocalDate): Day? {
        val version = plan.planAt(Slot(date, 0)) ?: return null
        return projectDay(
            date = date,
            weekday = date.dayOfWeek,
            plan = PlanVersion(plan.meals(version.id), plan.components(version.id), plan.items(version.id)),
            state = itemQueries.listForDate(date.toString()).executeAsList(),
        )
    }

    suspend fun tickOff(date: LocalDate, planItemId: String) =
        recordItem(date, planItemId) { it.copy(ticked_at = clock.nowMillis()) }

    suspend fun markPrepped(date: LocalDate, planItemId: String) =
        recordItem(date, planItemId) { it.copy(prepped_at = clock.nowMillis()) }

    suspend fun markCommitted(date: LocalDate, planItemId: String) =
        recordItem(date, planItemId) { it.copy(committed_at = clock.nowMillis()) }

    suspend fun setExcluded(date: LocalDate, planItemId: String, excluded: Boolean) =
        recordItem(date, planItemId) { it.copy(excluded = excluded) }

    private suspend fun recordItem(
        date: LocalDate,
        planItemId: String,
        change: (Day_item) -> Day_item,
    ) {
        val now = clock.nowMillis()
        val existing = itemQueries.findByDateAndPlanItem(date.toString(), planItemId).executeAsOneOrNull()
        val row = change(
            existing ?: Day_item(
                id = newId(null),
                user_id = auth.requireUserId(),
                plan_item_id = planItemId,
                updated_at = now,
                deleted_at = null,
                committed_at = null,
                prepped_at = null,
                ticked_at = null,
                date = date.toString(),
                excluded = false,
            ),
        ).copy(updated_at = now)

        db.writeWithOutbox(outbox, Tables.DAY_ITEM, row.id, OutboxOp.UPSERT) {
            itemQueries.upsert(row)
            row.toPayload()
        }
    }
}

private fun Day_item.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "plan_item_id" to JsonPrimitive(plan_item_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "committed_at" to JsonPrimitive(committed_at),
    "prepped_at" to JsonPrimitive(prepped_at),
    "ticked_at" to JsonPrimitive(ticked_at),
    "date" to JsonPrimitive(date),
    "excluded" to JsonPrimitive(excluded),
)
