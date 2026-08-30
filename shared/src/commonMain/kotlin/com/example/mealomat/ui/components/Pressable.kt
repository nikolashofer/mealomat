package com.example.mealomat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.example.mealomat.ui.theme.MealomatTheme

@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: CornerBasedShape,
    fill: Color,
    edge: Color,
    depth: Dp,
    enabled: Boolean = true,
): Modifier {
    val motion = MealomatTheme.motion
    val opacity = MealomatTheme.opacity
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val edgeDepth by animateDpAsState(
        targetValue = if (pressed) motion.pressedEdge else depth,
        animationSpec = tween(motion.pressMillis, easing = motion.pressEasing),
    )

    return this
        .alpha(if (enabled) 1f else opacity.disabled)
        .padding(bottom = depth)
        .offset { IntOffset(0, (depth - edgeDepth).roundToPx()) }
        .edge(edgeDepth, shape, edge)
        .clip(shape)
        .background(fill)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                waitForUpOrCancellation()
                pressed = false
            }
        }
        .clickable(
            interactionSource = null,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}
