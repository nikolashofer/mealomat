package com.example.mealomat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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

    Row(
        modifier = modifier
            .pressable(onClick, spec.shape, tone.fill, tone.edge, spec.edge, enabled)
            .padding(spec.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S8, Alignment.CenterHorizontally),
        content = content,
    )
}

@Composable
private fun ButtonSize.spec(): ButtonSpec {
    val t = MealomatTheme.shapes.button
    val s = MealomatTheme.shadows
    val label = MealomatTheme.typography.label
    return when (this) {
        ButtonSize.Sm -> ButtonSpec(t.sm, s.edgeSm.offsetY, PaddingValues(Space.S14, Space.S10), label.sm)
        ButtonSize.Md -> ButtonSpec(t.md, s.edgeMd.offsetY, PaddingValues(Space.S16, Space.S12), label.md)
        ButtonSize.Lg -> ButtonSpec(t.lg, s.edgeLg.offsetY, PaddingValues(Space.S24, Space.S16), label.lg)
    }
}
