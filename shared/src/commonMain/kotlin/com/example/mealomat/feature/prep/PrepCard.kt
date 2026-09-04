package com.example.mealomat.feature.prep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mealomat.feature.prep.model.PrepIngredient
import com.example.mealomat.feature.prep.model.PrepLine
import com.example.mealomat.feature.prep.model.PrepPortion
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.caps

private val TileHeight = 56.dp
private val TileFloor = 24.dp

@Composable
fun PrepCard(line: PrepLine, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = colors.tone.prep
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
        MakeBlock(line)
        if (line.portions.isNotEmpty()) Portions(line.portions)
    }
}

@Composable
private fun MakeBlock(line: PrepLine) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = colors.tone.prep
    val shape = MealomatTheme.shapes.control.lg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.canvas)
            .padding(horizontal = Space.S16, vertical = Space.S14),
        verticalArrangement = Arrangement.spacedBy(Space.S10),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.S4)) {
            BasicText(text = "MAKE", style = typography.label.xxs.caps().copy(color = tone.onTint))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.S6)) {
                BasicText(
                    text = line.make.value,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1,
                    style = typography.display.lg.copy(color = tone.onTint),
                )
                BasicText(
                    text = line.make.unit,
                    modifier = Modifier.alignByBaseline(),
                    style = typography.display.xs.copy(color = tone.fill),
                )
            }
        }
        if (line.ingredients.size > 1) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(Space.S2)
                    .clip(MealomatTheme.shapes.pill)
                    .background(colors.border.subtle),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Space.S6)) {
                line.ingredients.forEach { IngredientRow(it) }
            }
        }
    }
}

@Composable
private fun IngredientRow(ingredient: PrepIngredient) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = ingredient.name,
            modifier = Modifier.alignByBaseline(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.body.sm.copy(color = colors.text.primary),
        )
        BasicText(
            text = ingredient.amount.text,
            modifier = Modifier.alignByBaseline(),
            style = typography.label.md.copy(color = colors.tone.prep.onTint),
        )
    }
}

@Composable
private fun Portions(portions: List<PrepPortion>) {
    val typography = MealomatTheme.typography
    val tone = MealomatTheme.colors.tone.prep
    val boxes = if (portions.size == 1) "box" else "boxes"

    Column(verticalArrangement = Arrangement.spacedBy(Space.S8)) {
        BasicText(
            text = "Split into ${portions.size} $boxes",
            style = typography.label.xxs.caps().copy(color = tone.tint),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.S6),
            verticalAlignment = Alignment.Bottom,
        ) {
            portions.forEach { Tile(it) }
        }
    }
}

@Composable
private fun RowScope.Tile(portion: PrepPortion) {
    val typography = MealomatTheme.typography
    val tone = MealomatTheme.colors.tone.prep
    val height = TileFloor + (TileHeight - TileFloor) * portion.share

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(MealomatTheme.shapes.control.sm)
                .background(tone.onFill.copy(alpha = 0.16f))
                .padding(bottom = Space.S6),
            contentAlignment = Alignment.BottomCenter,
        ) {
            portion.amount?.let {
                BasicText(text = it.value, style = typography.label.sm.copy(color = tone.onFill))
            }
        }
        BasicText(
            text = portion.weekday,
            style = typography.label.xxs.caps().copy(color = tone.border),
        )
    }
}
