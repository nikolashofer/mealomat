package com.example.mealomat.ui.theme.semantic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.Space

@Immutable
data class MealomatSpacing(
    val inset: InsetSpacing = InsetSpacing(),
    val gap: GapSpacing = GapSpacing(),
)

@Immutable
data class InsetSpacing(
    val page: PaddingValues = PaddingValues(horizontal = Space.S20, vertical = Space.S20),
    val frame: PaddingValues = PaddingValues(horizontal = Space.S20, vertical = Space.S20),
    val card: PaddingValues = PaddingValues(Space.S16),
    val sheet: PaddingValues = PaddingValues(horizontal = Space.S24, vertical = Space.S28),
)

@Immutable
data class GapSpacing(
    val form: Dp = Space.S12,
    val button: Dp = Space.S12,
)

@Composable
fun PaddingValues.topOnly(): PaddingValues = onlyVertical(bottom = 0.dp)

@Composable
fun PaddingValues.bottomOnly(): PaddingValues = onlyVertical(top = 0.dp)

@Composable
private fun PaddingValues.onlyVertical(
    top: Dp = calculateTopPadding(),
    bottom: Dp = calculateBottomPadding(),
): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = top,
        end = calculateEndPadding(direction),
        bottom = bottom,
    )
}
