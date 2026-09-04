package com.example.mealomat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
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

private class ButtonSpec(
    val shape: CornerBasedShape,
    val edge: Dp,
    val height: Dp,
    val padding: PaddingValues,
    val textStyle: TextStyle,
)

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    size: ControlSize = ControlSize.Md,
    enabled: Boolean = true,
) {
    val style = size.spec().textStyle.copy(color = tone.onFill)
    Button(onClick, tone, modifier, size, enabled) {
        BasicText(text = text, style = style)
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    size: ControlSize = ControlSize.Md,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val spec = size.spec()

    Row(
        modifier = modifier
            .pressable(onClick, spec.shape, tone.fill, tone.edge, spec.edge, enabled)
            .heightIn(min = spec.height)
            .padding(spec.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S8, Alignment.CenterHorizontally),
        content = content,
    )
}

@Composable
private fun ControlSize.spec(): ButtonSpec {
    val s = MealomatTheme.shapes.control
    val e = MealomatTheme.shadows.edge
    val h = MealomatTheme.sizes.control
    val t = MealomatTheme.typography.label
    return when (this) {
        ControlSize.Sm -> ButtonSpec(s.sm, e.sm.offsetY, h.sm, PaddingValues(horizontal = Space.S14), t.sm)
        ControlSize.Md -> ButtonSpec(s.md, e.md.offsetY, h.md, PaddingValues(horizontal = Space.S16), t.md)
        ControlSize.Lg -> ButtonSpec(s.lg, e.lg.offsetY, h.lg, PaddingValues(horizontal = Space.S24), t.lg)
    }
}
