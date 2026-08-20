package com.example.mealomat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.mealomat.ui.theme.MealomatTheme

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secure: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography.field
    val motion = MealomatTheme.motion
    val shape = MealomatTheme.shapes.field

    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()

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
            .clip(shape)
            .background(colors.surface.raised)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BasicText(
            text = label.uppercase(),
            style = typography.label.copy(color = labelColor),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            enabled = enabled,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = typography.value.copy(
                color = colors.text.primary,
                letterSpacing = if (secure) 0.2.em else typography.value.letterSpacing,
            ),
            cursorBrush = SolidColor(colors.border.selected),
            visualTransformation =
                if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
        )
    }
}
