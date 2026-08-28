package com.example.mealomat.data.sync

import kotlin.time.Clock
import com.example.mealomat.data.db.MealomatDatabase

// TODO: maybe move to dedicated file
enum class OutboxOp { UPSERT, DELETE }

class OutboxWriter(
    private val db: MealomatDatabase,
    private val clock: Clock,
) {
    fun enqueue(tableName: String, rowId: String, op: OutboxOp, payload: String) {
        db.syncOutboxQueries.enqueue(
            table_name = tableName,
            row_id = rowId,
            op = op.name,
            payload = payload,
            created_at = clock.now().toEpochMilliseconds(),
        )
    }
}
