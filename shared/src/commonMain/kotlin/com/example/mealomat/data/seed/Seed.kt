package com.example.mealomat.data.seed

import com.example.mealomat.data.db.Basis
import kotlinx.serialization.Serializable

@Serializable
data class Seed(
    val version: Int,
    val ingredients: List<SeedIngredient> = emptyList(),
    val prepBlocks: List<SeedPrepBlock> = emptyList(),
    val planMeals: List<SeedMeal> = emptyList(),
)

@Serializable
data class SeedIngredient(
    val key: String,
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
    val note: String? = null,
)

@Serializable
data class SeedPrepBlock(
    val key: String,
    val name: String,
    val prepWeekday: Int,
    val shoppingWeekday: Int,
    val coversFromWeekday: Int,
    val coversFromPosition: Int,
)

@Serializable
data class SeedMeal(
    val weekday: Int,
    val name: String,
    val position: Int,
    val items: List<SeedItem> = emptyList(),
)

@Serializable
data class SeedItem(
    val ingredient: String,
    val amount: Double,
)
