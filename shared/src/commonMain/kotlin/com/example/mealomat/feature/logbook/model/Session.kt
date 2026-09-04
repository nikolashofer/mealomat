package com.example.mealomat.feature.logbook.model

import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.PrepStep

enum class SessionKind { Shopping, Prep }

data class Session(
    val kind: SessionKind,
    val blockId: String,
    val status: SessionStatus,
    val done: Int,
    val skipped: Int,
    val total: Int,
) {
    val got: Int get() = done - skipped
}

fun shoppingSession(
    blockId: String,
    status: SessionStatus,
    needs: List<IngredientNeed>,
    steps: List<Shopping_step>,
): Session {
    val settled = steps.map { it.ingredient_id }.toSet()
    return Session(
        kind = SessionKind.Shopping,
        blockId = blockId,
        status = status,
        done = steps.size,
        skipped = steps.count { it.bought_amount == null },
        total = steps.size + needs.count { it.buy > 0.0 && it.ingredientId !in settled },
    )
}

fun prepSession(blockId: String, status: SessionStatus, steps: List<PrepStep>) = Session(
    kind = SessionKind.Prep,
    blockId = blockId,
    status = status,
    done = steps.count { it.doneAt != null },
    // Prep has nothing to pass over: a step is either made or it is not.
    skipped = 0,
    total = steps.size,
)
