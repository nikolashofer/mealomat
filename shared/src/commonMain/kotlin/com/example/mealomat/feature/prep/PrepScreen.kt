package com.example.mealomat.feature.prep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealomat.feature.prep.model.PrepLine
import com.example.mealomat.feature.prep.model.PrepLineState
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ControlSize
import com.example.mealomat.ui.components.Icon
import com.example.mealomat.ui.components.IconImage
import com.example.mealomat.ui.components.Mascot
import com.example.mealomat.ui.components.MascotImage
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.bottomOnly
import com.example.mealomat.ui.theme.semantic.toDp
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PrepScreen(
    blockId: String,
    date: LocalDate,
    onClose: () -> Unit,
    viewModel: PrepViewModel = koinViewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val colors = MealomatTheme.colors

    LaunchedEffect(blockId, date) { viewModel.start(blockId, date) }

    val finished = ready && active == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface.canvas)
            .statusBarsPadding(),
    ) {
        when {
            finished -> SummaryCard(lines)
            else -> Header(lines, active, onClose)
        }

        val listState = rememberLazyListState()

        LaunchedEffect(active?.key, lines.size) {
            val index = lines.indexOfFirst { it.key == active?.key }
            if (index >= 0) listState.scrollToItem(index)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = Space.S20, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S10),
        ) {
            // The summary's body is not designed yet, so a finished session shows the card alone.
            if (!finished) {
                items(lines, key = { it.key }) { line ->
                    when {
                        line.key == active?.key -> PrepCard(line)
                        line.state == PrepLineState.Pending -> PendingRow(line) { viewModel.focus(line) }
                        else -> DoneRow(line)
                    }
                }
            }
        }

        when (val line = active) {
            null -> if (ready) Finished(onClose)
            else -> Footer(line, viewModel)
        }
    }
}

@Composable
private fun SummaryCard(lines: List<PrepLine>) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val tone = colors.tone.prep
    val shape = MealomatTheme.shapes.surface.card
    val depth = MealomatTheme.shadows.edge.lg.offsetY
    val steps = if (lines.size == 1) "step" else "steps"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MealomatTheme.spacing.inset.frame.bottomOnly())
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.lg.copy(color = tone.edge), shape)
            .clip(shape)
            .background(tone.fill)
            .padding(Space.S16),
        horizontalArrangement = Arrangement.spacedBy(Space.S14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MascotImage(
            mascot = Mascot.Excited,
            contentDescription = null,
            modifier = Modifier.size(MealomatTheme.sizes.mascot.header),
        )
        Column {
            BasicText(text = "Prep done", style = typography.display.sm.copy(color = tone.onFill))
            BasicText(
                text = "${lines.size} $steps",
                style = typography.body.sm.copy(color = tone.tint),
            )
        }
    }
}

@Composable
private fun Header(lines: List<PrepLine>, active: PrepLine?, onClose: () -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val done = lines.count { it.state == PrepLineState.Done }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MealomatTheme.spacing.inset.frame.bottomOnly()),
        verticalArrangement = Arrangement.spacedBy(Space.S12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.S12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "Prep session",
                    style = typography.label.lg.copy(color = colors.text.primary),
                )
                BasicText(
                    text = "$done of ${lines.size} done",
                    style = typography.body.xs.copy(color = colors.text.secondary),
                )
            }
            Button(
                text = "Pause",
                onClick = onClose,
                tone = colors.tone.neutral,
                size = ControlSize.Sm,
            )
        }
        ProgressBar(lines, active)
    }
}

@Composable
private fun ProgressBar(lines: List<PrepLine>, active: PrepLine?) {
    val colors = MealomatTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        lines.forEach { line ->
            val fill = when {
                line.key == active?.key -> colors.tone.prep.border
                line.state == PrepLineState.Done -> colors.tone.prep.fill
                else -> colors.border.subtle
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Space.S6)
                    .clip(MealomatTheme.shapes.pill)
                    .background(fill),
            )
        }
    }
}

@Composable
private fun DoneRow(line: PrepLine) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val shape = MealomatTheme.shapes.surface.card
    val depth = MealomatTheme.shadows.edge.sm.offsetY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(MealomatTheme.opacity.muted)
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.sm.copy(color = colors.edge.subtle), shape)
            .clip(shape)
            .background(colors.surface.raised)
            .border(1.dp, colors.border.subtle, shape)
            .padding(horizontal = Space.S14, vertical = Space.S12),
        horizontalArrangement = Arrangement.spacedBy(Space.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MealomatTheme.sizes.check)
                .clip(MealomatTheme.shapes.surface.badge)
                .background(colors.tone.prep.fill),
            contentAlignment = Alignment.Center,
        ) {
            IconImage(
                icon = Icon.Check,
                tint = colors.tone.prep.onFill,
                contentDescription = null,
                modifier = Modifier.size(typography.label.sm.fontSize.toDp()),
            )
        }
        BasicText(
            text = line.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.strong.md.copy(color = colors.text.primary),
        )
        BasicText(
            text = line.make.text,
            style = typography.label.sm.copy(color = colors.tone.prep.edge),
        )
    }
}

@Composable
private fun PendingRow(line: PrepLine, onClick: () -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val shape = MealomatTheme.shapes.surface.card
    val depth = MealomatTheme.shadows.edge.sm.offsetY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.sm.copy(color = colors.edge.subtle), shape)
            .clip(shape)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .background(colors.surface.raised)
            .border(1.dp, colors.border.subtle, shape)
            .padding(horizontal = Space.S14, vertical = Space.S14),
        horizontalArrangement = Arrangement.spacedBy(Space.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = line.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.body.md.copy(color = colors.text.primary),
        )
        BasicText(
            text = line.make.text,
            style = typography.strong.sm.copy(color = colors.text.secondary),
        )
    }
}

@Composable
private fun Footer(active: PrepLine, viewModel: PrepViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S20)
            .padding(top = Space.S12, bottom = Space.S24),
    ) {
        Button(
            text = "Made ${active.make.text}",
            onClick = { viewModel.make(active) },
            tone = MealomatTheme.colors.tone.prep,
            modifier = Modifier.weight(1f),
            size = ControlSize.Lg,
        )
    }
}

@Composable
private fun Finished(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S20)
            .padding(top = Space.S12, bottom = Space.S24),
    ) {
        Button(
            text = "Back to today",
            onClick = onClose,
            tone = MealomatTheme.colors.tone.prep,
            modifier = Modifier.weight(1f),
            size = ControlSize.Lg,
        )
    }
}
