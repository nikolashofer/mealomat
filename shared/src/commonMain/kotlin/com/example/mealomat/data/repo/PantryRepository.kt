package com.example.mealomat.data.repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.LedgerReason
import com.example.mealomat.data.db.LedgerSource
import com.example.mealomat.data.db.ListWithIngredient
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.db.Pantry_ledger
import com.example.mealomat.data.db.Pantry_stock
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import com.example.mealomat.domain.adjustDelta
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonPrimitive

data class PantryMove(
    val ingredientId: String,
    val delta: Double,
    val reason: LedgerReason,
    val sourceKind: LedgerSource = LedgerSource.MANUAL,
    val sourceId: String? = null,
    val occurredAt: Long? = null,
    val note: String? = null,
)

class PantryRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
) {
    private val ledgerQueries = db.pantryLedgerQueries
    private val stockQueries = db.pantryStockQueries

    fun observe(): Flow<List<ListWithIngredient>> =
        stockQueries.listWithIngredient().asFlow().mapToList(Dispatchers.Default)

    fun amountOf(ingredientId: String): Double =
        stockQueries.findByIngredient(ingredientId).executeAsOneOrNull()?.amount ?: 0.0

    fun movementsOf(ingredientId: String): List<Pantry_ledger> =
        ledgerQueries.listForIngredient(ingredientId).executeAsList()

    suspend fun consume(ingredientId: String, amount: Double, note: String? = null) =
        record(PantryMove(ingredientId, -amount, LedgerReason.CONSUME, note = note))

    suspend fun spoil(ingredientId: String, amount: Double, note: String? = null) =
        record(PantryMove(ingredientId, -amount, LedgerReason.SPOILAGE, note = note))

    suspend fun addAhead(ingredientId: String, amount: Double, note: String? = null) =
        record(PantryMove(ingredientId, amount, LedgerReason.BUY, note = note))

    suspend fun adjustTo(ingredientId: String, realAmount: Double, note: String? = null) =
        record(
            PantryMove(
                ingredientId = ingredientId,
                delta = adjustDelta(amountOf(ingredientId), realAmount),
                reason = LedgerReason.ADJUST,
                note = note,
            ),
        )

    suspend fun record(move: PantryMove): String =
        record(move, auth.requireUserId(), clock.nowMillis())

    internal fun record(move: PantryMove, userId: String, now: Long): String {
        val row = move.toRow(newId(null), userId, now)
        db.transaction {
            db.writeWithOutbox(outbox, Tables.PANTRY_LEDGER, row.id, OutboxOp.UPSERT) {
                ledgerQueries.insert(row)
                row.toPayload()
            }
            stockQueries.upsert(Pantry_stock(row.ingredient_id, amountOf(row.ingredient_id) + row.delta, now))
        }
        return row.id
    }

    fun rebuildStock() = db.transaction {
        stockQueries.clear()
        stockQueries.rebuild(clock.nowMillis())
    }
}

private fun PantryMove.toRow(id: String, userId: String, now: Long) = Pantry_ledger(
    id = id,
    user_id = userId,
    ingredient_id = ingredientId,
    updated_at = now,
    occurred_at = occurredAt ?: now,
    delta = delta,
    reason = reason,
    source_kind = sourceKind,
    source_id = sourceId,
    note = note,
)

private fun Pantry_ledger.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "ingredient_id" to JsonPrimitive(ingredient_id),
    "updated_at" to JsonPrimitive(updated_at),
    "occurred_at" to JsonPrimitive(occurred_at),
    "delta" to JsonPrimitive(delta),
    "reason" to JsonPrimitive(reason.name),
    "source_kind" to JsonPrimitive(source_kind.name),
    "source_id" to JsonPrimitive(source_id),
    "note" to JsonPrimitive(note),
)
