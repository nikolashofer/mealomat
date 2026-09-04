package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MealomatSizes(
    val control: ControlSizes = ControlSizes(),
    val mascot: MascotSizes = MascotSizes(),
    // TODO: remove, i.e. check should be control.xs
    val check: Dp = 26.dp,
    val badge: Dp = 20.dp,
)

@Immutable
data class ControlSizes(
    val sm: Dp = 38.dp,
    val md: Dp = 46.dp,
    val lg: Dp = 55.dp,
)

@Immutable
data class MascotSizes(
    val hero: Dp = 160.dp,
    val header: Dp = 48.dp,
)
