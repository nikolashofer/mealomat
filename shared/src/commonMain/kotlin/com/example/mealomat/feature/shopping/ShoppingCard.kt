package com.example.mealomat.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.example.mealomat.domain.MeasureLabel
import com.example.mealomat.feature.shopping.model.ShoppingLine
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.caps

@Composable
fun ShoppingCard(line: ShoppingLine, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = colors.tone.shopping
    val shape = MealomatTheme.shapes.surface.card
    val depth = MealomatTheme.shadows.edge.lg.offsetY

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.lg.copy(color = tone.edge), shape)
            .clip(shape)
            .background(tone.fill)
            .padding(Space.S20),
        verticalArrangement = Arrangement.spacedBy(Space.S14),
    ) {
        BasicText(
            text = line.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.display.md.copy(color = tone.onFill),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S10)) {
            Figure("NEED", line.need)
            Figure("HAVE", line.have)
        }
        BuyBlock(line.buy)
    }
}

@Composable
private fun RowScope.Figure(label: String, value: MeasureLabel) {
    val tone = MealomatTheme.colors.tone.shopping
    val typography = MealomatTheme.typography

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MealomatTheme.shapes.control.md)
            .background(tone.onFill.copy(alpha = 0.16f))
            .padding(horizontal = Space.S14, vertical = Space.S12),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        BasicText(text = label, style = typography.label.xxs.caps().copy(color = tone.tint))
        BasicText(
            text = value.text,
            maxLines = 1,
            style = typography.display.xs.copy(color = tone.onFill),
        )
    }
}

@Composable
private fun BuyBlock(buy: MeasureLabel) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = colors.tone.shopping
    val shape = MealomatTheme.shapes.control.lg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.canvas)
            .padding(horizontal = Space.S16, vertical = Space.S14),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        BasicText(text = "BUY", style = typography.label.xxs.caps().copy(color = tone.fill))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S6)) {
            BasicText(
                text = buy.value,
                modifier = Modifier.alignByBaseline(),
                maxLines = 1,
                style = typography.display.lg.copy(color = tone.fill),
            )
            BasicText(
                text = buy.unit,
                modifier = Modifier.alignByBaseline(),
                style = typography.display.xs.copy(color = tone.fill),
            )
        }
    }
}
