package com.example.mealomat.data.seed

import com.example.mealomat.data.repo.IngredientDraft
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PlanItemDraft
import com.example.mealomat.data.repo.PlanMealDraft
import com.example.mealomat.data.repo.PlanRepository
import com.example.mealomat.data.repo.PrepBlockDraft
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.domain.Slot
import kotlinx.datetime.DayOfWeek

// TODO: redo proper seeds file, once seeded, so we do not need this Seed<Table> models 1:1 export/import
// TODO: write tests for all seeds stuff, once properly done
class SeedImporter(
    private val ingredients: IngredientRepository,
    private val plan: PlanRepository,
    private val prepBlocks: PrepBlockRepository,
) {
    suspend fun import(seed: Seed, activeFrom: Slot) {
        val ingredientIds = seed.ingredients.associate { it.key to ingredients.upsert(it.toDraft()) }

        seed.prepBlocks.forEach { prepBlocks.upsert(it.toDraft()) }

        val planId = plan.create(activeFrom)
        seed.planMeals.forEach { meal ->
            val mealId = plan.upsertMeal(planId, meal.toDraft())
            meal.items.forEachIndexed { index, item ->
                val ingredientId = requireNotNull(ingredientIds[item.ingredient]) {
                    "seed references unknown ingredient '${item.ingredient}'"
                }
                plan.upsertItem(
                    planId,
                    PlanItemDraft(
                        planMealId = mealId,
                        ingredientId = ingredientId,
                        amount = item.amount,
                        position = index,
                    ),
                )
            }
        }
    }
}

private fun SeedIngredient.toDraft() = IngredientDraft(
    name = name,
    basis = basis,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    saturatedFatG = saturatedFatG,
    saltG = saltG,
    packSize = packSize,
    note = note,
)

private fun SeedPrepBlock.toDraft() = PrepBlockDraft(
    name = name,
    prepWeekday = DayOfWeek(prepWeekday),
    shoppingWeekday = DayOfWeek(shoppingWeekday),
    coversFromWeekday = DayOfWeek(coversFromWeekday),
    coversFromPosition = coversFromPosition,
)

private fun SeedMeal.toDraft() = PlanMealDraft(
    weekday = DayOfWeek(weekday),
    name = name,
    position = position,
)
