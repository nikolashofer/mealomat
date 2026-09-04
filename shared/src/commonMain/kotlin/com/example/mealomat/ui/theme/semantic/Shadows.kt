package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.primitives.Palette

@Immutable
data class MealomatShadows(
    val edge: EdgeShadows = EdgeShadows(),
    val soft: Shadow = Shadow(offsetY = 12.dp, blur = 28.dp, color = Palette.Navy800.copy(alpha = 0.16f)),
)

@Immutable
data class EdgeShadows(
    val sm: Shadow = Shadow(offsetY = 3.dp),
    val md: Shadow = Shadow(offsetY = 4.dp),
    val lg: Shadow = Shadow(offsetY = 5.dp),
)

@Immutable
data class Shadow(
    val offsetY: Dp,
    val blur: Dp = 0.dp,
    val spread: Dp = 0.dp,
    val color: Color = Color.Unspecified,
)