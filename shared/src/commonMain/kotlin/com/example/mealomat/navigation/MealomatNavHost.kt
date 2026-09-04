package com.example.mealomat.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.mealomat.feature.logbook.LogbookScreen
import com.example.mealomat.feature.logbook.model.SessionKind
import com.example.mealomat.feature.prep.PrepScreen
import com.example.mealomat.feature.shopping.ShoppingScreen
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MealomatNavHost(navBar: NavBarViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selected by navBar.selected.collectAsStateWithLifecycle()

    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // TODO: do transitions properly
        NavHost(
            navController = navController,
            startDestination = Logbook,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable<Logbook> {
                LogbookScreen(
                    date = selected,
                    contentPadding = PaddingValues(bottom = barHeight),
                    onSession = { session ->
                        val date = selected.toString()
                        navController.navigate(
                            when (session.kind) {
                                SessionKind.Shopping -> Shopping(session.blockId, date)
                                SessionKind.Prep -> Prep(session.blockId, date)
                            },
                        )
                    },
                )
            }
            composable<Prep> { entry ->
                val route = entry.toRoute<Prep>()
                PrepScreen(
                    blockId = route.blockId,
                    date = LocalDate.parse(route.date),
                    onClose = { navController.popBackStack() },
                )
            }
            composable<Shopping> { entry ->
                val route = entry.toRoute<Shopping>()
                ShoppingScreen(
                    blockId = route.blockId,
                    date = LocalDate.parse(route.date),
                    onClose = { navController.popBackStack() },
                )
            }
        }

        if (entry?.destination.showsNavBar()) {
            NavBar(
                days = navBar.days,
                selected = selected,
                onSelect = navBar::select,
                onPlan = navBar::signOut,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                    .fillMaxWidth()
                    .padding(horizontal = Space.S20)
                    .padding(bottom = Space.S20),
            )
        }
    }
}
