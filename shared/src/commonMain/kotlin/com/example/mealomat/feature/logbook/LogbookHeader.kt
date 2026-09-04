package com.example.mealomat.feature.logbook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.example.mealomat.data.db.SessionStatus
import com.example.mealomat.domain.DayTotals
import com.example.mealomat.domain.gramsValue
import com.example.mealomat.domain.kcalLabel
import com.example.mealomat.feature.logbook.model.MealRow
import com.example.mealomat.feature.logbook.model.SessionKind
import com.example.mealomat.feature.logbook.model.Session
import com.example.mealomat.feature.logbook.model.eatenPercent
import com.example.mealomat.feature.logbook.model.moodBlurb
import com.example.mealomat.feature.logbook.model.nextMeal
import com.example.mealomat.ui.components.Icon
import com.example.mealomat.ui.components.IconImage
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.components.pressable
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.ToneColors
import com.example.mealomat.ui.theme.semantic.toDp
import com.example.mealomat.ui.theme.semantic.bottomOnly

@Composable
fun LogbookHeader(
    totals: DayTotals,
    meals: List<MealRow>,
    sessions: List<Session>,
    onSession: (Session) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MealomatTheme.colors
    val percent = eatenPercent(totals)

    Column(modifier = modifier.fillMaxWidth().background(colors.surface.canvas)) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(MealomatTheme.spacing.inset.frame.bottomOnly()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.S16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CalorieLine(totals)
                    MacroRow(totals, Modifier.padding(bottom = Space.S16))
                    Bubble(percent, meals)
                }
                RingWithBadge(percent)
            }

            if (sessions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.S20),
                    horizontalArrangement = Arrangement.spacedBy(Space.S10),
                ) {
                    sessions.forEach { SessionButton(it, onSession, Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.fillMaxWidth().height(Space.S2).background(colors.border.subtle))
    }
}

@Composable
private fun CalorieLine(totals: DayTotals) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography

    Row(
        horizontalArrangement = Arrangement.spacedBy(Space.S8),
    ) {
        BasicText(
            text = kcalLabel(totals.eaten.kcal).value,
            modifier = Modifier.alignByBaseline(),
            style = typography.display.lg.copy(color = colors.text.primary),
        )
        BasicText(
            text = "/ ${kcalLabel(totals.planned.kcal).text}",
            modifier = Modifier.alignByBaseline(),
            style = typography.strong.sm.copy(color = colors.text.tertiary),
        )
    }
}

@Composable
private fun MacroRow(totals: DayTotals, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Space.S14)) {
        MacroColumn("P", totals.eaten.proteinG, totals.planned.proteinG)
        MacroColumn("C", totals.eaten.carbsG, totals.planned.carbsG)
        MacroColumn("F", totals.eaten.fatG, totals.planned.fatG)
    }
}

@Composable
private fun RowScope.MacroColumn(label: String, eaten: Double, planned: Double) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val fraction by animateFloatAsState(
        targetValue = if (planned <= 0.0) 0f else (eaten / planned).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(ProgressMillis),
    )

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        BasicText(
            modifier = Modifier,
            text = buildAnnotatedString {
                append("$label ${gramsValue(eaten)}")
                withStyle(SpanStyle(color = colors.text.tertiary)) { append("/${gramsValue(planned)}") }
            },
            maxLines = 1,
            style = typography.label.xs.copy(color = colors.text.primary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Space.S6)
                .clip(MealomatTheme.shapes.pill)
                .background(colors.border.subtle),
        ) {
            Spacer(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.text.primary))
        }
    }
}

// TODO: make speech bubble
@Composable
private fun Bubble(percent: Int, meals: List<MealRow>) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val next = nextMeal(meals)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface.subtle, MealomatTheme.shapes.control.sm)
            .padding(horizontal = Space.S12, vertical = Space.S8),
        horizontalArrangement = Arrangement.spacedBy(Space.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = if (next == null) "All ticked!" else "${moodBlurb(percent)} · ${next.name}",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.label.xs.copy(color = colors.text.primary),
        )
    }
}

@Composable
private fun RingWithBadge(percent: Int) {
    val colors = MealomatTheme.colors
    val success = colors.status.success
    val shape = MealomatTheme.shapes.surface.badge

    Box(modifier = Modifier.size(RingDiameter), contentAlignment = Alignment.BottomCenter) {
        FuelRing(percent)
        Row(
            modifier = Modifier
                .offset(y = Space.S6)
                .height(MealomatTheme.sizes.check)
                .edge(MealomatTheme.shadows.edge.sm.copy(color = success.edge), shape)
                .clip(shape)
                .background(success.fill)
                .padding(horizontal = Space.S10),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "$percent%",
                style = MealomatTheme.typography.label.xs.copy(color = colors.text.strong),
            )
        }
    }
}

@Composable
private fun SessionButton(
    session: Session,
    onSession: (Session) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone: ToneColors = when (session.kind) {
        SessionKind.Shopping -> colors.tone.shopping
        SessionKind.Prep -> colors.tone.prep
    }

    val abandoned = session.status == SessionStatus.ABANDONED
    val fill = if (abandoned) colors.tone.neutral.fill else tone.fill
    val edge = if (abandoned) colors.tone.neutral.edge else tone.edge
    val label = if (abandoned) colors.text.secondary else tone.onFill
    val sub = if (abandoned) colors.text.secondary else tone.tint

    val row = Modifier
        .heightIn(min = MealomatTheme.sizes.control.lg)
        .padding(horizontal = Space.S14)

    Row(
        modifier = modifier
            .then(
                when (session.status) {
                    SessionStatus.IN_PROGRESS -> Modifier.pressable(
                        onClick = { onSession(session) },
                        shape = MealomatTheme.shapes.control.lg,
                        fill = fill,
                        edge = edge,
                        depth = MealomatTheme.shadows.edge.lg.offsetY,
                    )
                    else -> Modifier
                        .padding(bottom = MealomatTheme.shadows.edge.lg.offsetY)
                        .edge(MealomatTheme.shadows.edge.lg.copy(color = edge), MealomatTheme.shapes.control.lg)
                        .clip(MealomatTheme.shapes.control.lg)
                        .background(fill)
                },
            )
            .then(row),
        horizontalArrangement = Arrangement.spacedBy(Space.S10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = session.title(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = typography.label.md.copy(color = label),
            )
            BasicText(
                text = session.subtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = typography.strong.xs.copy(color = sub),
            )
        }
        if (session.status == SessionStatus.IN_PROGRESS) {
            IconImage(
                icon = Icon.CaretRight,
                tint = label,
                contentDescription = null,
                modifier = Modifier.size(typography.label.md.fontSize.toDp()),
            )
        }
    }
}

private fun Session.title(): String = when (status) {
    SessionStatus.DONE -> when (kind) {
        SessionKind.Shopping -> "Shopped"
        SessionKind.Prep -> "Prepped"
    }
    else -> when (kind) {
        SessionKind.Shopping -> "Shop"
        SessionKind.Prep -> "Prep"
    }
}

private fun Session.subtitle(): String = when (status) {
    SessionStatus.IN_PROGRESS -> when (kind) {
        SessionKind.Shopping -> "$done of $total bought"
        SessionKind.Prep -> "$done of $total steps"
    }
    SessionStatus.DONE -> when {
        kind == SessionKind.Prep -> "All $total steps"
        skipped > 0 -> "$got got · $skipped skipped"
        else -> "All $total bought"
    }
    SessionStatus.ABANDONED -> "Left at $done of $total"
}

