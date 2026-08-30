package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MealomatSizes(
    val mascot: MascotSizes = MascotSizes(),
    val nav: NavSizes = NavSizes(),
    val check: Dp = 26.dp,
    val badge: Dp = 20.dp,
)

@Immutable
data class NavSizes(
    val dot: Dp = 6.dp,
    val selectedDotWidth: Dp = 16.dp,
    val divider: Dp = 32.dp,
)

@Immutable
data class MascotSizes(
    val hero: Dp = 160.dp,
    val header: Dp = 48.dp,
)
