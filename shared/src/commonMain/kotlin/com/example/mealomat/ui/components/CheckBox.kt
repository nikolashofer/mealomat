package com.example.mealomat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.semantic.ToneColors
import com.example.mealomat.ui.theme.semantic.toDp

@Composable
fun CheckBox(
    checked: Boolean,
    tone: ToneColors,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MealomatTheme.colors
    val motion = MealomatTheme.motion
    val shape = MealomatTheme.shapes.surface.badge
    val spec = tween<Color>(motion.pressMillis, easing = motion.pressEasing)

    val fill by animateColorAsState(
        targetValue = if (checked) tone.fill else colors.surface.raised,
        animationSpec = spec,
    )
    val border by animateColorAsState(
        targetValue = if (checked) tone.fill else colors.border.strong,
        animationSpec = spec,
    )
    val edge by animateColorAsState(
        targetValue = if (checked) tone.edge else colors.edge.subtle,
        animationSpec = spec,
    )

    Box(
        modifier = modifier
            .pressable(
                pressed = pressed,
                shape = shape,
                fill = fill,
                edge = edge,
                depth = MealomatTheme.shadows.edge.sm.offsetY,
                enabled = enabled,
            )
            .size(MealomatTheme.sizes.check)
            .border(2.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            IconImage(
                icon = Icon.Check,
                tint = tone.onFill,
                contentDescription = null,
                modifier = Modifier.size(MealomatTheme.typography.label.sm.fontSize.toDp()),
            )
        }
    }
}
