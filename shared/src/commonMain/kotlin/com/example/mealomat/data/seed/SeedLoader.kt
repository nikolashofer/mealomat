package com.example.mealomat.data.seed

import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PlanRepository
import com.example.mealomat.domain.Slot
import com.example.mealomat.domain.weekStart
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.example.mealomat.dev.DevSetup
import kotlinx.serialization.json.Json
import mealomat.shared.generated.resources.Res

private const val SEED_PATH = "files/seed.json"

class SeedOnFirstRun(
    private val importer: SeedImporter,
    private val ingredients: IngredientRepository,
    private val plan: PlanRepository,
    private val clock: Clock,
    private val load: suspend () -> Seed = ::loadBundledSeed,
) : DevSetup {
    override suspend fun run() {
        if (!isEmpty()) return
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        importer.import(load(), Slot(weekStart(today), 0))
    }

    private fun isEmpty() = ingredients.isEmpty() && plan.isEmpty()
}

private val json = Json { ignoreUnknownKeys = true }

private suspend fun loadBundledSeed(): Seed =
    json.decodeFromString(Seed.serializer(), Res.readBytes(SEED_PATH).decodeToString())
