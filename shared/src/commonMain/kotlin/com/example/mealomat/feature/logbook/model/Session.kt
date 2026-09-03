package com.example.mealomat.feature.logbook.model

import com.example.mealomat.data.db.Shopping_step
import com.example.mealomat.domain.IngredientNeed
import com.example.mealomat.domain.PrepStep

enum class SessionKind { Shopping, Prep }

data class Session(
    val kind: SessionKind,
    val blockId: String,
    val done: Int,
    val total: Int,
)

fun shoppingSession(blockId: String, needs: List<IngredientNeed>, steps: List<Shopping_step>) = Session(
    kind = SessionKind.Shopping,
    blockId = blockId,
    done = steps.size,
    total = steps.size + needs.count { it.buy > 0.0 },
)

fun prepSession(blockId: String, steps: List<PrepStep>) = Session(
    kind = SessionKind.Prep,
    blockId = blockId,
    done = steps.count { it.doneAt != null },
    total = steps.size,
)
