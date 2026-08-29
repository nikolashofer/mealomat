package com.example.mealomat.data.repo

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.requireUserId
import com.example.mealomat.data.db.Basis
import com.example.mealomat.data.db.Ingredient
import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import com.example.mealomat.data.sync.Tables
import com.example.mealomat.data.sync.payloadOf
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonPrimitive

data class IngredientDraft(
    val id: String? = null,
    val name: String,
    val basis: Basis,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val saturatedFatG: Double? = null,
    val saltG: Double? = null,
    val packSize: Double? = null,
    val archived: Boolean = false,
    val note: String? = null,
)

class IngredientRepository(
    private val db: MealomatDatabase,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val clock: Clock,
) {
    private val queries = db.ingredientQueries

    fun observe(): Flow<List<Ingredient>> =
        queries.list().asFlow().mapToList(Dispatchers.Default)

    fun observeWithArchived(): Flow<List<Ingredient>> =
        queries.listWithArchived().asFlow().mapToList(Dispatchers.Default)

    fun byId(id: String): Ingredient? = queries.findById(id).executeAsOneOrNull()

    fun isEmpty(): Boolean = queries.countWithArchived().executeAsOne() == 0L

    suspend fun upsert(draft: IngredientDraft): String {
        val row = draft.toRow(newId(draft.id), auth.requireUserId(), clock.nowMillis())
        db.writeWithOutbox(outbox, Tables.INGREDIENT, row.id, OutboxOp.UPSERT) {
            queries.upsert(row)
            row.toPayload()
        }
        return row.id
    }

    suspend fun softDelete(id: String) {
        val now = clock.nowMillis()
        db.writeWithOutbox(outbox, Tables.INGREDIENT, id, OutboxOp.DELETE) {
            queries.softDelete(deleted_at = now, updated_at = now, id = id)
            queries.findById(id).executeAsOneOrNull()?.toPayload()
        }
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val now = clock.nowMillis()
        db.writeWithOutbox(outbox, Tables.INGREDIENT, id, OutboxOp.UPSERT) {
            queries.setArchived(archived = archived, updated_at = now, id = id)
            queries.findById(id).executeAsOneOrNull()?.toPayload()
        }
    }
}

private fun IngredientDraft.toRow(id: String, userId: String, now: Long) = Ingredient(
    id = id,
    user_id = userId,
    updated_at = now,
    deleted_at = null,
    name = name,
    basis = basis,
    kcal = kcal,
    protein_g = proteinG,
    carbs_g = carbsG,
    fat_g = fatG,
    fiber_g = fiberG,
    sugar_g = sugarG,
    saturated_fat_g = saturatedFatG,
    salt_g = saltG,
    pack_size = packSize,
    archived = archived,
    note = note,
)

private fun Ingredient.toPayload() = payloadOf(
    "id" to JsonPrimitive(id),
    "user_id" to JsonPrimitive(user_id),
    "updated_at" to JsonPrimitive(updated_at),
    "deleted_at" to JsonPrimitive(deleted_at),
    "name" to JsonPrimitive(name),
    "basis" to JsonPrimitive(basis.name),
    "kcal" to JsonPrimitive(kcal),
    "protein_g" to JsonPrimitive(protein_g),
    "carbs_g" to JsonPrimitive(carbs_g),
    "fat_g" to JsonPrimitive(fat_g),
    "fiber_g" to JsonPrimitive(fiber_g),
    "sugar_g" to JsonPrimitive(sugar_g),
    "saturated_fat_g" to JsonPrimitive(saturated_fat_g),
    "salt_g" to JsonPrimitive(salt_g),
    "pack_size" to JsonPrimitive(pack_size),
    "archived" to JsonPrimitive(archived),
    "note" to JsonPrimitive(note),
)
