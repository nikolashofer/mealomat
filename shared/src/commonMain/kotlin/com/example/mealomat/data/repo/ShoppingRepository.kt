package com.example.mealomat.data.repo

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.data.db.Shopping_trip
import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.Window
import com.example.mealomat.domain.dates
import com.example.mealomat.domain.needsFrom
import com.example.mealomat.domain.usesIn
import com.example.mealomat.domain.windowOf
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonPrimitive

class ShoppingRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
    private val days: DayRepository,
    private val pantry: PantryRepository,
    private val ingredients: IngredientRepository,
    private val prepBlocks: PrepBlockRepository,
) {
    private val tripQueries = db.shoppingTripQueries
    private val stepQueries = db.shoppingStepQueries

    fun open(): Shopping_trip? = tripQueries.findOpen().executeAsOneOrNull()

    fun tripOn(prepBlockId: String, on: LocalDate): Shopping_trip? =
        tripQueries.findForBlock(prepBlockId, on.toString()).executeAsOneOrNull()

    fun stepsOf(tripId: String): List<Shopping_step> =
        stepQueries.listForTrip(tripId).executeAsList()

    fun committedWindows(from: LocalDate): List<Window> =
        tripQueries.listCommitted(from.toString()).executeAsList().map { it.window() }

    fun needsFor(prepBlockId: String, on: LocalDate): List<IngredientNeed> {
        val window = windowFor(prepBlockId, on) ?: return emptyList()
        return needsIn(window) { 0.0 }
    }

    fun needsOf(tripId: String): List<IngredientNeed> {
        val trip = tripQueries.findById(tripId).executeAsOneOrNull() ?: return emptyList()
        val bought = stepsOf(tripId).associate { it.ingredient_id to (it.bought_amount ?: 0.0) }
        return needsIn(trip.window()) { bought[it] ?: 0.0 }
    }

    suspend fun forBlock(prepBlockId: String, on: LocalDate): String {
        val now = clock.nowMillis()
        val window = requireNotNull(windowFor(prepBlockId, on)) { "No coverage window for $prepBlockId" }
        open()?.let { openTrip ->
            if (openTrip.window() == window) return openTrip.id
            closeForgotten(openTrip)
        }

        val row = Shopping_trip(
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
        db.writeWithOutbox(outbox, Tables.SHOPPING_TRIP, row.id, OutboxOp.UPSERT) {
            tripQueries.insert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun complete(tripId: String) =
        writeTrip(tripId) { trip, now -> trip.copy(status = SessionStatus.DONE, completed_at = now) }

    suspend fun abandon(tripId: String) =
        writeTrip(tripId) { trip, _ -> trip.copy(status = SessionStatus.ABANDONED) }

    suspend fun buyStep(tripId: String, ingredientId: String, amount: Double) =
        writeStep(tripId, ingredientId) { step, _ -> step.copy(bought_amount = amount, skipped_at = null) }

    suspend fun skipStep(tripId: String, ingredientId: String) =
        writeStep(tripId, ingredientId) { step, now -> step.copy(skipped_at = now, bought_amount = null) }

    private suspend fun writeTrip(
        tripId: String,
        change: (Shopping_trip, Long) -> Shopping_trip,
    ): String {
        val now = clock.nowMillis()
        val trip = requireNotNull(tripQueries.findById(tripId).executeAsOneOrNull()) { "No trip $tripId" }
        val row = change(trip, now).copy(updated_at = now)

        db.writeWithOutbox(outbox, Tables.SHOPPING_TRIP, row.id, OutboxOp.UPSERT) {
            tripQueries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    private suspend fun writeStep(
        tripId: String,
        ingredientId: String,
        change: (Shopping_step, Long) -> Shopping_step,
    ): String {
        val userId = auth.requireUserId()
        val now = clock.nowMillis()
        val previous = findStep(tripId, ingredientId)
        val row = change(previous ?: newStepRow(tripId, ingredientId, userId, now), now)
            .stamped(now, needsOf(tripId).firstOrNull { it.ingredientId == ingredientId })
        val move = moveFor(previous, row, now)

        db.transaction {
            db.writeWithOutbox(outbox, Tables.SHOPPING_STEP, row.id, OutboxOp.UPSERT) {
                stepQueries.upsert(row)
                row.toPayload()
            }
            if (move != null) pantry.record(move, userId = userId, now = now)
        }
        return row.id
    }

    private suspend fun closeForgotten(trip: Shopping_trip) {
        if (stepsOf(trip.id).any { it.bought_amount != null }) complete(trip.id) else abandon(trip.id)
    }

    private fun moveFor(previous: Shopping_step?, row: Shopping_step, now: Long): PantryMove? {
        val delta = (row.bought_amount ?: 0.0) - (previous?.bought_amount ?: 0.0)
        return if (delta == 0.0) null else PantryMove(
            ingredientId = row.ingredient_id,
            delta = delta,
            reason = LedgerReason.BUY,
            sourceKind = LedgerSource.SHOPPING_STEP,
            sourceId = row.id,
            occurredAt = now,
        )
    }

    private fun findStep(tripId: String, ingredientId: String) =
        stepQueries.findByTripAndIngredient(tripId, ingredientId).executeAsOneOrNull()

    private fun windowFor(prepBlockId: String, on: LocalDate): Window? {
        val blocks = prepBlocks.list()
        val block = blocks.firstOrNull { it.id == prepBlockId } ?: return null
        return windowOf(block, blocks, on)
    }

    private fun needsIn(window: Window, boughtHere: (String) -> Double): List<IngredientNeed> {
        val projected = window.dates().mapNotNull { days.byDate(it) }
        return needsFrom(
            uses = usesIn(window, projected),
            have = { pantry.amountOf(it) - boughtHere(it) },
            packSize = { ingredients.byId(it)?.pack_size },
        ).sortedBy { it.ingredientId }
    }
}

private fun Shopping_trip.window() = Window(
    from = Slot(LocalDate.parse(covers_from_date), covers_from_position.toInt()),
    to = Slot(LocalDate.parse(covers_to_date), covers_to_position.toInt()),
)

private fun newStepRow(tripId: String, ingredientId: String, userId: String, now: Long) = Shopping_step(
    id = newId(null),
    user_id = userId,
    shopping_trip_id = tripId,
    ingredient_id = ingredientId,
    updated_at = now,
    deleted_at = null,
    skipped_at = null,
    needed_amount = 0.0,
    have_amount = 0.0,
    suggested_amount = 0.0,
    bought_amount = null,
)

private fun Shopping_step.stamped(now: Long, need: IngredientNeed?) = copy(
    updated_at = now,
    needed_amount = need?.need ?: needed_amount,
    have_amount = need?.have ?: have_amount,
    suggested_amount = need?.buy ?: suggested_amount,
)

private fun Shopping_trip.toPayload() = payloadOf(
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

private fun Shopping_step.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "shopping_trip_id" to JsonPrimitive(shopping_trip_id),
    "ingredient_id" to JsonPrimitive(ingredient_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "skipped_at" to JsonPrimitive(skipped_at),
    "needed_amount" to JsonPrimitive(needed_amount),
    "have_amount" to JsonPrimitive(have_amount),
    "suggested_amount" to JsonPrimitive(suggested_amount),
    "bought_amount" to JsonPrimitive(bought_amount),
)
