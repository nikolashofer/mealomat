package com.example.mealomat.data.repo

import com.example.mealomat.data.db.MealomatDatabase
import com.example.mealomat.data.sync.OutboxOp
import com.example.mealomat.data.sync.OutboxWriter
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal fun Clock.nowMillis() = now().toEpochMilliseconds()

internal fun newId(given: String?) = given ?: Uuid.generateV7().toString()

// row and outbox entry are written together or not at all; a null payload skips the entry.
internal fun MealomatDatabase.writeWithOutbox(
    outbox: OutboxWriter,
    table: String,
    id: String,
    op: OutboxOp,
    write: () -> String?,
) = transaction {
    val payload = write() ?: return@transaction
    outbox.enqueue(table, id, op, payload)
}
