package com.example.mealomat.ui.theme.semantic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MealomatMotion(
    val pressMillis: Int = 90,
    val pressEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val pressedEdge: Dp = 1.dp,
)
