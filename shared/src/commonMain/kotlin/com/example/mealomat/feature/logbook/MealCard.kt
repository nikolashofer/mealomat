package com.example.mealomat.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space

@Composable
fun MealCard(meal: MealRow, onTick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val shape = MealomatTheme.shapes.card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .edge(MealomatTheme.shadows.edgeSm.copy(color = colors.edge.subtle), shape)
            .clip(shape)
            .background(colors.surface.raised)
            .border(1.dp, colors.border.subtle, shape)
            .padding(Space.S16),
        verticalArrangement = Arrangement.spacedBy(Space.S12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(meal.name, style = typography.display.sm.copy(color = colors.text.primary))
            BasicText(
                text = "${meal.kcal.toInt()} kcal",
                style = typography.number.unit.copy(color = colors.text.secondary),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Space.S10)) {
            meal.items.forEach { ItemLine(it, onTick) }
        }
    }
}

@Composable
private fun ItemLine(item: ItemRow, onTick: (String) -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val struck = item.ticked || item.excluded

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = !struck,
                onClick = { onTick(item.planItemId) },
            ),
        horizontalArrangement = Arrangement.spacedBy(Space.S10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.excluded) Box(Modifier.size(MealomatTheme.sizes.check)) else CheckBox(item.ticked)

        BasicText(
            text = buildAnnotatedString {
                append(item.name)
                withStyle(typography.item.amount.toSpanStyle().copy(color = colors.text.secondary)) {
                    append(" · ${item.amount}")
                }
            },
            modifier = Modifier.weight(1f),
            style = typography.item.name.copy(
                color = if (struck) colors.text.tertiary else colors.text.primary,
                textDecoration = if (struck) TextDecoration.LineThrough else null,
            ),
        )

        if (!item.excluded) SourceBadge(prepped = item.prepped)
    }
}

// TODO: make component out of this
@Composable
private fun CheckBox(ticked: Boolean) {
    val colors = MealomatTheme.colors
    val shape = MealomatTheme.shapes.chip

    Box(
        modifier = Modifier
            .size(MealomatTheme.sizes.check)
            .clip(shape)
            .background(if (ticked) colors.tone.logbook.fill else colors.surface.raised)
            .then(if (ticked) Modifier else Modifier.border(2.dp, colors.border.strong, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (ticked) {
            BasicText(
                text = "✓",
                style = MealomatTheme.typography.label.sm.copy(color = colors.tone.logbook.onFill),
            )
        }
    }
}

@Composable
private fun SourceBadge(prepped: Boolean) {
    val colors = MealomatTheme.colors

    Box(
        modifier = Modifier
            .size(MealomatTheme.sizes.badge)
            .clip(MealomatTheme.shapes.chip)
            .background(colors.tone.neutral.fill),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (prepped) "P" else "F",
            style = MealomatTheme.typography.field.label.copy(color = colors.tone.neutral.onFill),
        )
    }
}
