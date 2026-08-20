package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.primitives.Palette

@Immutable
data class Shadow(
    val offsetY: Dp,
    val blur: Dp = 0.dp,
    val spread: Dp = 0.dp,
    // supplied by caller for edges
    val color: Color = Color.Unspecified,
)

@Immutable
data class MealomatShadows(
    val edgeSm: Shadow = Shadow(offsetY = 3.dp),
    val edgeMd: Shadow = Shadow(offsetY = 4.dp),
    val edgeLg: Shadow = Shadow(offsetY = 5.dp),
    val soft: Shadow = Shadow(offsetY = 12.dp, blur = 28.dp, color = Palette.Navy800.copy(alpha = 0.16f)),
)
