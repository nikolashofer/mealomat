package com.example.mealomat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.semantic.ToneColors
import com.example.mealomat.ui.theme.Space

enum class ButtonSize { Sm, Md, Lg }

private class ButtonSpec(
    val shape: CornerBasedShape,
    val edge: Dp,
    val padding: PaddingValues,
    val textStyle: TextStyle,
)

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
) = Button(onClick, tone, modifier, size, enabled) {
    BasicText(text = text, style = size.spec().textStyle.copy(color = tone.onFill))
}

@Composable
fun Button(
    onClick: () -> Unit,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val spec = size.spec()
    val motion = MealomatTheme.motion
    val opacity = MealomatTheme.opacity
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val edgeDepth by animateDpAsState(
        targetValue = if (pressed) motion.pressedEdge else spec.edge,
        animationSpec = tween(motion.pressMillis, easing = motion.pressEasing),
    )
    val drop = spec.edge - edgeDepth

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else opacity.disabled)
            .padding(bottom = spec.edge),
    ) {
        Row(
            modifier = modifier
                .offset { IntOffset(0, drop.roundToPx()) }
                .drawBehind {
                    translate(top = edgeDepth.toPx()) {
                        drawOutline(
                            outline = spec.shape.createOutline(this.size, layoutDirection, this),
                            color = tone.edge,
                        )
                    }
                }
                .clip(spec.shape)
                .background(tone.fill)
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
                .padding(spec.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.S8, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

@Composable
private fun ButtonSize.spec(): ButtonSpec {
    val t = MealomatTheme.shapes.button
    val s = MealomatTheme.shadows
    val label = MealomatTheme.typography.label
    return when (this) {
        ButtonSize.Sm -> ButtonSpec(t.sm, s.edgeSm.offsetY, PaddingValues(Space.S14, Space.S10), label.xs)
        ButtonSize.Md -> ButtonSpec(t.md, s.edgeMd.offsetY, PaddingValues(Space.S16, Space.S12), label.md)
        ButtonSize.Lg -> ButtonSpec(t.lg, s.edgeLg.offsetY, PaddingValues(Space.S24, Space.S16), label.lg)
    }
}
