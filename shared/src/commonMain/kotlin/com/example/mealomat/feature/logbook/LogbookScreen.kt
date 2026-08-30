package com.example.mealomat.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LogbookScreen(
    date: LocalDate,
    contentPadding: PaddingValues,
    viewModel: LogbookViewModel = koinViewModel(),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val colors = MealomatTheme.colors

    LaunchedEffect(date) { viewModel.show(date) }

    Column(
        modifier = Modifier.background(colors.surface.canvas).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        totals?.let { LogbookHeader(date, it, sessions, modifier = Modifier.zIndex(1f)) }

        // TODO: proper empty state...
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = Space.S20, vertical = Space.S16),
            verticalArrangement = Arrangement.spacedBy(Space.S14),
        ) {
            meals.forEach { meal ->
                MealCard(meal, onTick = { viewModel.tick(date, it) })
            }
            if (totals == null) {
                BasicText(
                    text = "No plan for $date",
                    style = MealomatTheme.typography.label.lg.copy(color = colors.text.tertiary),
                )
            }
        }
    }
}
