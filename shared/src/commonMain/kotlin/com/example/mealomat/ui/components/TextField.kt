package com.example.mealomat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.caps

private class FieldSpec(
    val shape: CornerBasedShape,
    val height: Dp,
    val padding: PaddingValues,
    val textStyle: TextStyle,
)

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    size: ControlSize = ControlSize.Md,
    enabled: Boolean = true,
    secure: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography.label
    val motion = MealomatTheme.motion
    val spec = size.spec(label)

    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()

    // TODO: move focused behaviour to a shared modifier
    // TODO: and encode disabled behaviour
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = tween(motion.pressMillis, easing = motion.pressEasing),
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) colors.border.selected else colors.border.subtle,
        animationSpec = tween(motion.pressMillis, easing = motion.pressEasing),
    )
    val labelColor by animateColorAsState(
        targetValue = if (focused) colors.text.brand else colors.text.tertiary,
        animationSpec = tween(motion.pressMillis, easing = motion.pressEasing),
    )

    Column(
        modifier = modifier
            .clip(spec.shape)
            .background(colors.surface.raised)
            .border(borderWidth, borderColor, spec.shape)
            .height(spec.height)
            .padding(spec.padding),
        verticalArrangement = Arrangement.Center,
    ) {
        if (label != null) {
            BasicText(
                text = label.uppercase(),
                style = typography.xxs.caps().copy(color = labelColor),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            enabled = enabled,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = spec.textStyle.copy(
                color = colors.text.primary,
                letterSpacing = if (secure) 0.2.em else spec.textStyle.letterSpacing,
            ),
            cursorBrush = SolidColor(colors.border.selected),
            visualTransformation =
                if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
        )
    }
}

@Composable
private fun ControlSize.spec(label: String?): FieldSpec {
    val s = MealomatTheme.shapes.control
    val e = MealomatTheme.shadows.edge
    val h = MealomatTheme.sizes.control
    val t = MealomatTheme.typography.body
    return when (if (label == null) this else ControlSize.Lg) {
        ControlSize.Sm -> FieldSpec(s.sm, h.sm, PaddingValues(horizontal = Space.S14), t.sm)
        ControlSize.Md -> FieldSpec(s.md, h.md, PaddingValues(horizontal = Space.S16), t.md)
        ControlSize.Lg -> FieldSpec(s.lg, if (label == null) h.lg else h.lg + e.lg.offsetY, PaddingValues(horizontal = Space.S16), t.lg)
    }
}
