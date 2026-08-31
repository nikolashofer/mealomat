package com.example.mealomat.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.shadow.Shadow
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ButtonSize
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.components.pressable
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

@Composable
fun NavBar(
    days: List<DayNav>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MealomatTheme.colors
    val shapes = MealomatTheme.shapes
    val soft = MealomatTheme.shadows.soft
    val edge = MealomatTheme.shadows.edgeSm.offsetY

    Box(modifier = modifier.padding(bottom = edge)) {
        Row(
            modifier = Modifier
                .edge(MealomatTheme.shadows.edgeSm.copy(color = colors.edge.subtle), shapes.nav)
                .dropShadow(shapes.nav, Shadow(soft.blur, soft.color, offset = DpOffset(0.dp, soft.offsetY)))
                .clip(shapes.nav)
                .background(colors.surface.raised)
                .border(1.dp, colors.border.subtle, shapes.nav)
                .padding(horizontal = Space.S10, vertical = Space.S8),
            horizontalArrangement = Arrangement.spacedBy(Space.S2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            days.forEach { day ->
                DayNavButton(
                    day = day,
                    selected = day.date == selected,
                    onClick = { onSelect(day.date) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(
                modifier = Modifier
                    .padding(horizontal = Space.S10)
                    .width(1.dp)
                    .height(MealomatTheme.sizes.nav.divider)
                    .background(colors.border.subtle),
            )
            Button("Plan", onClick = onPlan, tone = colors.tone.neutral, size = ButtonSize.Md)
        }
    }
}

// TODO: not exactly same height as md button -> fix
// TODO: maybe render items always square with a hint that it can be scrolled, active one is always centered,
//  and display week somehow if user moves of current week, so basically infinite scroll list with projected days
@Composable
private fun DayNavButton(day: DayNav, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val tone = if (selected) colors.tone.brand else null

    Column(
        modifier = modifier
            .pressable(
                onClick = onClick,
                shape = MealomatTheme.shapes.button.md,
                fill = tone?.fill ?: Color.Transparent,
                edge = tone?.edge ?: Color.Transparent,
                depth = MealomatTheme.shadows.edgeMd.offsetY,
            )
            .padding(vertical = Space.S8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        BasicText(
            text = day.date.dayOfWeek.short(),
            style = MealomatTheme.typography.label.md.copy(
                color = when {
                    selected -> colors.tone.brand.onFill
                    day.isPast -> colors.text.tertiary
                    else -> colors.text.primary
                },
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
            when {
                selected -> Dot(colors.tone.brand.onFill, wide = true)
                day.shops || day.preps -> {
                    if (day.shops) Dot(colors.tone.shopping.fill)
                    if (day.preps) Dot(colors.tone.prep.fill)
                }
                else -> Dot(colors.text.faint)
            }
        }
    }
}

@Composable
private fun Dot(color: Color, wide: Boolean = false) {
    val sizes = MealomatTheme.sizes.nav
    Box(
        modifier = Modifier
            .size(if (wide) sizes.selectedDotWidth else sizes.dot, sizes.dot)
            .clip(MealomatTheme.shapes.pill)
            .background(color),
    )
}

private fun DayOfWeek.short() = when (this) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Tu"
    DayOfWeek.WEDNESDAY -> "We"
    DayOfWeek.THURSDAY -> "Th"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "Su"
}
