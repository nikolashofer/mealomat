package com.example.mealomat.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.mealomat.domain.DayTotals
import com.example.mealomat.domain.grams
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ButtonSize
import com.example.mealomat.ui.components.Mascot
import com.example.mealomat.ui.components.MascotImage
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlin.math.roundToInt

@Composable
fun LogbookHeader(
    date: LocalDate,
    totals: DayTotals,
    sessions: List<SessionTile>,
    modifier: Modifier = Modifier,
) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val shape = MealomatTheme.shapes.header

    Column(
        modifier = modifier
            .fillMaxWidth()
            .edge(MealomatTheme.shadows.edgeMd.copy(color = colors.edge.subtle), shape)
            .clip(shape)
            .background(colors.surface.raised)
            .statusBarsPadding()
            .padding(bottom = Space.S20)
            // .padding(top = Space.S20, bottom = Space.S20)
            .padding(horizontal = Space.S20),
        verticalArrangement = Arrangement.spacedBy(Space.S12),
    ) {
        // TODO: pretty redundant
        /*Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = date.format(HEADER_DATE).uppercase(),
                style = typography.label.caps.copy(color = colors.text.tertiary),
            )
            BasicText(
                text = "W26 · DAY ${date.dayOfWeek.isoDayNumber}",
                modifier = Modifier
                    .clip(MealomatTheme.shapes.pill)
                    .background(colors.tone.neutral.fill)
                    .padding(horizontal = Space.S10, vertical = Space.S4),
                style = typography.field.label.copy(color = colors.text.secondary),
            )
        }*/

        // TODO: fix alignment of numbers with mascot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.S10),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicText(
                text = totals.eaten.kcal.roundToInt().grouped(),
                style = typography.number.lg.copy(color = colors.text.primary),
            )
            BasicText(
                text = "/ ${totals.planned.kcal.roundToInt().grouped()} kcal",
                modifier = Modifier.padding(bottom = Space.S6).weight(1f),
                style = typography.number.unit.copy(color = colors.text.tertiary),
            )
            // TODO: should react on actions -> make mascot feel alive
            MascotImage(
                mascot = Mascot.Happy,
                contentDescription = null,
                modifier = Modifier.size(MealomatTheme.sizes.mascot.header),
            )
        }

        MacroBar(totals)
        Legend(totals)

        if (sessions.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = Space.S4),
                verticalArrangement = Arrangement.spacedBy(Space.S14),
            ) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.border.subtle))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.S8)) {
                    sessions.forEach { SessionButton(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MacroBar(totals: DayTotals) {
    val colors = MealomatTheme.colors
    val remaining = (totals.planned.grams - totals.eaten.grams).coerceAtLeast(0.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Space.S12)
            .clip(MealomatTheme.shapes.pill)
            .background(colors.border.subtle),
        horizontalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        Segment(totals.eaten.proteinG, colors.macro.protein)
        Segment(totals.eaten.carbsG, colors.macro.carbs)
        Segment(totals.eaten.fatG, colors.macro.fat)
        Segment(remaining, colors.border.subtle)
    }
}

// TODO: mybe min widht so 0 actually already shows stuff in the bar
@Composable
private fun RowScope.Segment(grams: Double, color: Color) {
    if (grams <= 0.0) return
    Spacer(modifier = Modifier.weight(grams.toFloat()).fillMaxHeight().background(color))
}

@Composable
private fun Legend(totals: DayTotals) {
    val colors = MealomatTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.S14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendEntry("P", colors.macro.protein, totals.eaten.proteinG, totals.planned.proteinG)
        LegendEntry("C", colors.macro.carbs, totals.eaten.carbsG, totals.planned.carbsG)
        LegendEntry("F", colors.macro.fat, totals.eaten.fatG, totals.planned.fatG)
    }
}

@Composable
private fun LegendEntry(label: String, color: Color, eaten: Double, planned: Double) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography

    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.S6),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(Space.S10)
                .clip(MealomatTheme.shapes.pill)
                .background(color),
        )
        BasicText(
            text = buildAnnotatedString {
                append("$label ")
                withStyle(SpanStyle(color = colors.text.primary, fontWeight = typography.label.xs.fontWeight)) {
                    append(eaten.roundToInt().toString())
                }
                append(" / ${planned.roundToInt()}")
            },
            style = typography.label.soft.copy(color = colors.text.secondary),
        )
    }
}

private val HEADER_DATE = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
    chars(" · ")
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
}

// space between thousands
private fun Int.grouped(): String =
    toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
private fun SessionButton(session: SessionTile, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = when (session.kind) {
        SessionKind.Shopping -> colors.tone.shopping
        SessionKind.Prep -> colors.tone.prep
    }

    Button(onClick = {}, tone = tone, modifier = modifier, size = ButtonSize.Md) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(
                text = when (session.kind) {
                    SessionKind.Shopping -> "Shopping trip"
                    SessionKind.Prep -> "Prep session"
                },
                style = typography.label.lg.copy(color = tone.onFill),
            )
            BasicText(
                text = when (session.kind) {
                    SessionKind.Shopping -> "${session.done} of ${session.total} items bought"
                    SessionKind.Prep -> "${session.done} of ${session.total} steps done"
                },
                style = typography.field.label.copy(color = tone.tint),
            )
        }
        // TODO: make icon
        BasicText("›", style = typography.display.sm.copy(color = tone.onFill))
    }
}
