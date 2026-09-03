package com.example.mealomat.feature.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import com.example.mealomat.feature.shopping.model.LineState
import com.example.mealomat.feature.shopping.model.ShoppingLine
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ControlSize
import com.example.mealomat.ui.components.Icon
import com.example.mealomat.ui.components.edge
import com.example.mealomat.ui.components.IconImage
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import com.example.mealomat.ui.theme.semantic.bottomOnly
import com.example.mealomat.ui.theme.semantic.toDp
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShoppingScreen(
    blockId: String,
    date: LocalDate,
    onClose: () -> Unit,
    viewModel: ShoppingViewModel = koinViewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val colors = MealomatTheme.colors

    LaunchedEffect(blockId, date) { viewModel.start(blockId, date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface.canvas)
            .statusBarsPadding(),
    ) {
        Header(lines, active, onClose)

        val listState = rememberLazyListState()

        LaunchedEffect(active?.ingredientId, lines.size) {
            val index = lines.indexOfFirst { it.ingredientId == active?.ingredientId }
            if (index >= 0) listState.animateScrollToItem(index)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = Space.S20, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S10),
        ) {
            items(lines, key = { it.ingredientId }) { line ->
                when {
                    line.ingredientId == active?.ingredientId -> ShoppingCard(line)
                    line.state == LineState.Pending -> PendingRow(line) { viewModel.focus(line) }
                    else -> SettledRow(line) { viewModel.focus(line) }
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
private fun Header(lines: List<ShoppingLine>, active: ShoppingLine?, onClose: () -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val done = lines.count { it.state == LineState.Bought }

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
            Column(
                modifier = Modifier.weight(1f),
            ) {
                BasicText(
                    text = "Shopping trip",
                    style = typography.label.lg.copy(color = colors.text.primary),
                )
                BasicText(
                    text = "$done of ${lines.size} bought",
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
private fun ProgressBar(lines: List<ShoppingLine>, active: ShoppingLine?) {
    val colors = MealomatTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        lines.forEach { line ->
            val fill = when {
                line.ingredientId == active?.ingredientId -> colors.tone.shopping.border
                line.state == LineState.Bought -> colors.tone.shopping.fill
                line.state == LineState.Skipped -> colors.tone.neutral.edge
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
private fun SettledRow(line: ShoppingLine, onClick: () -> Unit) {
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography
    val bought = line.state == LineState.Bought
    val shape = MealomatTheme.shapes.surface.card
    val edge = if (bought) colors.edge.subtle else colors.tone.neutral.edge
    val depth = MealomatTheme.shadows.edge.sm.offsetY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(MealomatTheme.opacity.muted)
            .padding(bottom = depth)
            .edge(MealomatTheme.shadows.edge.sm.copy(color = edge), shape)
            .clip(shape)
            .then(
                when {
                    bought -> Modifier
                    else -> Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                },
            )
            .background(if (bought) colors.surface.raised else colors.surface.subtle)
            .border(1.dp, if (bought) colors.border.subtle else colors.tone.neutral.border, shape)
            .padding(horizontal = Space.S14, vertical = Space.S12),
        horizontalArrangement = Arrangement.spacedBy(Space.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MealomatTheme.sizes.check)
                .clip(MealomatTheme.shapes.surface.badge)
                .background(if (bought) colors.tone.shopping.fill else colors.tone.neutral.border),
            contentAlignment = Alignment.Center,
        ) {
            IconImage(
                icon = if (bought) Icon.Check else Icon.Minus,
                tint = if (bought) colors.tone.shopping.onFill else colors.text.secondary,
                contentDescription = null,
                modifier = Modifier.size(typography.label.sm.fontSize.toDp()),
            )
        }
        BasicText(
            text = line.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.strong.md.copy(
                color = if (bought) colors.text.primary else colors.text.secondary,
            ),
        )
        when (val amount = line.bought) {
            null -> BasicText(
                text = "skipped",
                style = typography.strong.sm.copy(color = colors.text.tertiary),
            )
            else -> BasicText(
                text = "+${amount.text}",
                style = typography.label.sm.copy(color = colors.tone.shopping.edge),
            )
        }
    }
}

@Composable
private fun PendingRow(line: ShoppingLine, onClick: () -> Unit) {
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
            text = line.buy.text,
            style = typography.strong.sm.copy(color = colors.text.secondary),
        )
    }
}

@Composable
private fun Footer(active: ShoppingLine, viewModel: ShoppingViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S20)
            .padding(top = Space.S12, bottom = Space.S24),
        horizontalArrangement = Arrangement.spacedBy(Space.S10),
    ) {
        Button(
            text = "Skip",
            onClick = { viewModel.skip(active) },
            tone = MealomatTheme.colors.tone.neutral,
            modifier = Modifier.width(Space.S112),
            size = ControlSize.Lg,
        )
        Button(
            text = "Bought ${active.buy.text}",
            onClick = { viewModel.buy(active) },
            tone = MealomatTheme.colors.tone.shopping,
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
            tone = MealomatTheme.colors.tone.brand,
            modifier = Modifier.weight(1f),
            size = ControlSize.Lg,
        )
    }
}
