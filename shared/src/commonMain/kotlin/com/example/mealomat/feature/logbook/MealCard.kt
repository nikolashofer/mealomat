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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mealomat.domain.kcalLabel
import com.example.mealomat.feature.logbook.model.isComplete
import com.example.mealomat.feature.logbook.model.ItemRow
import com.example.mealomat.feature.logbook.model.MealRow
import com.example.mealomat.ui.components.CheckBox
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.components.pressGesture
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.ToneColors
import com.example.mealomat.ui.theme.semantic.caps

@Composable
fun MealCard(
    meal: MealRow,
    number: Int,
    open: Boolean,
    active: Boolean,
    onToggle: () -> Unit,
    onTick: (ItemRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MealomatTheme.colors
    val shape = MealomatTheme.shapes.surface.card
    val edge = if (active) colors.tone.brand.fill else colors.edge.subtle
    val depth = MealomatTheme.shadows.edge.sm.offsetY

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.sm.copy(color = edge), shape)
            .clip(shape)
            .background(colors.surface.raised)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) colors.tone.brand.fill else colors.border.subtle,
                shape = shape,
            ),
    ) {
        MealHeader(meal, number, active, onToggle)
        if (open) {
            Column(modifier = Modifier.padding(start = Space.S14, end = Space.S14, bottom = Space.S10)) {
                meal.items.forEach { ItemLine(it, onTick) }
            }
        }
    }
}

@Composable
private fun MealHeader(meal: MealRow, number: Int, active: Boolean, onToggle: () -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val done = meal.isComplete()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onToggle)
            .padding(horizontal = Space.S14, vertical = Space.S12),
        horizontalArrangement = Arrangement.spacedBy(Space.S10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MealBadge(number, done, active)
        BasicText(
            text = meal.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.label.lg.copy(
                color = if (done) colors.text.secondary else colors.text.primary,
            ),
        )
        BasicText(
            text = kcalLabel(meal.kcal).text,
            style = typography.label.sm.copy(color = colors.text.tertiary),
        )
    }
}

@Composable
private fun MealBadge(number: Int, done: Boolean, active: Boolean) {
    val colors = MealomatTheme.colors
    val tone: ToneColors? = when {
        done -> colors.tone.logbook
        active -> colors.tone.brand
        else -> null
    }

    Box(
        modifier = Modifier
            .size(MealomatTheme.sizes.check)
            .clip(MealomatTheme.shapes.surface.badge)
            .background(tone?.fill ?: colors.surface.subtle),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = number.toString(),
            style = MealomatTheme.typography.label.xs.copy(
                color = tone?.onFill ?: colors.text.tertiary,
            ),
        )
    }
}

@Composable
private fun ItemLine(item: ItemRow, onTick: (ItemRow) -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val struck = item.ticked || item.excluded
    var pressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressGesture { pressed = it }
            .clickable(interactionSource = null, indication = null) { onTick(item) }
            .padding(vertical = Space.S8),
        horizontalArrangement = Arrangement.spacedBy(Space.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(checked = item.ticked, tone = colors.tone.logbook, pressed = pressed)
        BasicText(
            text = item.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.strong.sm.copy(
                color = if (struck) colors.text.tertiary else colors.text.primary,
            ),
        )
        BasicText(
            text = item.amount,
            style = typography.body.sm.copy(color = colors.text.tertiary),
        )
        SourceBadge(item.prepped)
    }
}

@Composable
private fun SourceBadge(prepped: Boolean) {
    val colors = MealomatTheme.colors

    Box(
        modifier = Modifier
            .size(MealomatTheme.sizes.badge)
            .clip(MealomatTheme.shapes.surface.badge)
            .background(if (prepped) colors.tone.prep.tint else colors.tone.neutral.fill),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (prepped) "P" else "F",
            style = MealomatTheme.typography.label.xxs.caps().copy(
                color = if (prepped) colors.tone.prep.onTint else colors.text.tertiary,
            ),
        )
    }
}
