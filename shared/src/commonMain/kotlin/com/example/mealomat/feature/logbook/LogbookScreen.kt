package com.example.mealomat.feature.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealomat.feature.logbook.model.Session
import com.example.mealomat.feature.logbook.model.nextMeal
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LogbookScreen(
    date: LocalDate,
    contentPadding: PaddingValues,
    onSession: (Session) -> Unit,
    viewModel: LogbookViewModel = koinViewModel(),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val open by viewModel.openMeals.collectAsStateWithLifecycle()

    LaunchedEffect(date) { viewModel.show(date) }

    Column(modifier = Modifier.background(MealomatTheme.colors.surface.canvas).fillMaxSize()) {
        totals?.let { LogbookHeader(it, meals, sessions, onSession, modifier = Modifier.zIndex(1f)) }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(MealomatTheme.spacing.inset.page),
            verticalArrangement = Arrangement.spacedBy(Space.S10),
        ) {
            val active = nextMeal(meals)?.id
            meals.forEachIndexed { index, meal ->
                MealCard(
                    meal = meal,
                    number = index + 1,
                    open = meal.id in open,
                    active = meal.id == active,
                    onToggle = { viewModel.toggleMeal(meal.id) },
                    onTick = { viewModel.tick(date, it.planItemId, ticked = !it.ticked) },
                )
            }
            // TODO: better empty state
            if (meals.isEmpty()) {
                BasicText(
                    text = "No plan for $date",
                    style = MealomatTheme.typography.label.lg.copy(color = MealomatTheme.colors.text.tertiary),
                )
            }
        }
    }
}
