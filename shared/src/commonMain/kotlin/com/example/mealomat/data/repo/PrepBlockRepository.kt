package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Prep_block
import com.example.mealomat.data.db.Prep_step_override
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlin.time.Clock
import kotlinx.serialization.json.JsonPrimitive

data class PrepBlockDraft(
    val id: String? = null,
    val name: String,
    val prepWeekday: DayOfWeek,
    val shoppingWeekday: DayOfWeek,
    val coversFromWeekday: DayOfWeek,
    val coversFromPosition: Int,
)

data class PrepStepOverrideDraft(
    val id: String? = null,
    val prepBlockId: String,
    val targetKey: String,
    val position: Int,
)

class PrepBlockRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
) {
    private val blockQueries = db.prepBlockQueries
    private val overrideQueries = db.prepStepOverrideQueries

    fun blocks(): List<Prep_block> = blockQueries.list().executeAsList()

    fun overrides(prepBlockId: String): List<Prep_step_override> =
        overrideQueries.listForBlock(prepBlockId).executeAsList()

    suspend fun upsertBlock(draft: PrepBlockDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PREP_BLOCK, row.id, OutboxOp.UPSERT) {
            blockQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun upsertOverride(draft: PrepStepOverrideDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.PREP_STEP_OVERRIDE, row.id, OutboxOp.UPSERT) {
            overrideQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

}

private fun PrepBlockDraft.toRow(id: String, userId: String, now: Long) = Prep_block(
    id = id,
    user_id = userId,
    updated_at = now,
    deleted_at = null,
    name = name,
    prep_weekday = prepWeekday,
    shopping_weekday = shoppingWeekday,
    covers_from_weekday = coversFromWeekday,
    covers_from_position = coversFromPosition.toLong(),
)

private fun PrepStepOverrideDraft.toRow(id: String, userId: String, now: Long) = Prep_step_override(
    id = id,
    user_id = userId,
    prep_block_id = prepBlockId,
    updated_at = now,
    deleted_at = null,
    target_key = targetKey,
    position = position.toLong(),
)

private fun Prep_block.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "name" to JsonPrimitive(name),
    "prep_weekday" to JsonPrimitive(prep_weekday.isoDayNumber),
    "shopping_weekday" to JsonPrimitive(shopping_weekday.isoDayNumber),
    "covers_from_weekday" to JsonPrimitive(covers_from_weekday.isoDayNumber),
    "covers_from_position" to JsonPrimitive(covers_from_position),
)

private fun Prep_step_override.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "prep_block_id" to JsonPrimitive(prep_block_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "target_key" to JsonPrimitive(target_key),
    "position" to JsonPrimitive(position),
)
