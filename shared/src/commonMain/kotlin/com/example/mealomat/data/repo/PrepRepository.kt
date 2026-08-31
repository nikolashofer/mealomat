package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Prep_session
import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.PrepStep
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.Window
import com.example.mealomat.domain.dates
import com.example.mealomat.domain.prepStepsIn
import com.example.mealomat.domain.windowOf
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive

class PrepRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
    private val days: DayRepository,
    private val ingredients: IngredientRepository,
    private val prepBlocks: PrepBlockRepository,
) {
    private val sessionQueries = db.prepSessionQueries

    fun open(): Prep_session? = sessionQueries.findOpen().executeAsOneOrNull()

    fun stepsOf(sessionId: String): List<PrepStep> {
        val session = sessionQueries.findById(sessionId).executeAsOneOrNull() ?: return emptyList()
        return stepsIn(session.window(), session.prep_block_id)
    }

    fun stepsFor(prepBlockId: String, on: LocalDate): List<PrepStep> {
        val window = windowFor(prepBlockId, on) ?: return emptyList()
        return stepsIn(window, prepBlockId)
    }

    fun committedWindows(from: LocalDate): List<Window> =
        sessionQueries.listCommitted(from.toString()).executeAsList().map { it.window() }

    suspend fun forBlock(prepBlockId: String, on: LocalDate): String {
        val now = clock.nowMillis()
        val window = requireNotNull(windowFor(prepBlockId, on)) { "No coverage window for $prepBlockId" }
        open()?.let { openSession ->
            if (openSession.window() == window) return openSession.id
            closeForgotten(openSession)
        }

        val row = Prep_session(
            id = newId(null), user_id = auth.requireUserId(), prep_block_id = prepBlockId,
            updated_at = now, deleted_at = null,
            started_at = now, completed_at = null,
            planned_date = on.toString(),
            covers_from_date = window.from.date.toString(),
            covers_from_position = window.from.position.toLong(),
            covers_to_date = window.to.date.toString(),
            covers_to_position = window.to.position.toLong(),
            status = SessionStatus.IN_PROGRESS,
        )
        db.writeWithOutbox(outbox, Tables.PREP_SESSION, row.id, OutboxOp.UPSERT) {
            sessionQueries.insert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun complete(sessionId: String) =
        writeSession(sessionId) { session, now -> session.copy(status = SessionStatus.DONE, completed_at = now) }

    suspend fun abandon(sessionId: String) =
        writeSession(sessionId) { session, _ -> session.copy(status = SessionStatus.ABANDONED) }

    suspend fun completeStep(sessionId: String, stepKey: String) {
        val step = requireNotNull(stepsOf(sessionId).firstOrNull { it.key == stepKey }) { "No step $stepKey on session $sessionId" }
        step.items.filter { it.preppedAt == null }.forEach { days.markPrepped(it.date, it.planItemId) }
    }

    suspend fun reorder(prepBlockId: String, keys: List<String>) {
        val existing = prepBlocks.overridesOf(prepBlockId).associateBy { it.target_key }
        keys.forEachIndexed { position, key ->
            prepBlocks.upsertOverride(
                PrepStepOverrideDraft(
                    id = existing[key]?.id,
                    prepBlockId = prepBlockId,
                    targetKey = key,
                    position = position,
                ),
            )
        }
    }

    private suspend fun writeSession(
        sessionId: String,
        change: (Prep_session, Long) -> Prep_session,
    ): String {
        val now = clock.nowMillis()
        val session = requireNotNull(sessionQueries.findById(sessionId).executeAsOneOrNull()) { "No session $sessionId" }
        val row = change(session, now).copy(updated_at = now)

        db.writeWithOutbox(outbox, Tables.PREP_SESSION, row.id, OutboxOp.UPSERT) {
            sessionQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    private suspend fun closeForgotten(session: Prep_session) {
        val prepped = stepsOf(session.id).any { step -> step.items.any { it.preppedAt != null } }
        if (prepped) complete(session.id) else abandon(session.id)
    }

    private fun stepsIn(window: Window, prepBlockId: String): List<PrepStep> {
        val projected = window.dates().mapNotNull { days.byDate(it) }
        val components = projected.flatMap { it.meals }.flatMap { it.components }
            .associate { "component:${it.lineage_id}" to it.name }
        return prepStepsIn(window, projected, prepBlocks.overridesOf(prepBlockId)) { key ->
            components[key] ?: ingredients.byId(key.removePrefix("ingredient:"))?.name ?: key
        }
    }

    private fun windowFor(prepBlockId: String, on: LocalDate): Window? {
        val blocks = prepBlocks.list()
        val block = blocks.firstOrNull { it.id == prepBlockId } ?: return null
        return windowOf(block, blocks, on)
    }
}

private fun Prep_session.window() = Window(
    from = Slot(LocalDate.parse(covers_from_date), covers_from_position.toInt()),
    to = Slot(LocalDate.parse(covers_to_date), covers_to_position.toInt()),
)

private fun Prep_session.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "prep_block_id" to JsonPrimitive(prep_block_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "started_at" to JsonPrimitive(started_at),
    "completed_at" to JsonPrimitive(completed_at),
    "planned_date" to JsonPrimitive(planned_date),
    "covers_from_date" to JsonPrimitive(covers_from_date),
    "covers_from_position" to JsonPrimitive(covers_from_position),
    "covers_to_date" to JsonPrimitive(covers_to_date),
    "covers_to_position" to JsonPrimitive(covers_to_position),
    "status" to JsonPrimitive(status.name),
)
