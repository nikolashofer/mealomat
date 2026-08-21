package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MealomatSizes(
    val mascot: MascotSizes = MascotSizes(),
)

@Immutable
data class MascotSizes(
    val hero: Dp = 160.dp,
)
