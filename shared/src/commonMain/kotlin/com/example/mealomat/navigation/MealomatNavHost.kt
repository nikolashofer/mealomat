package com.example.mealomat.navigation

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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.mealomat.feature.logbook.LogbookScreen
import com.example.mealomat.ui.theme.Space
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MealomatNavHost(navBar: NavBarViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selected = entry?.takeIf { it.destination.showsNavBar() }?.toRoute<Logbook>()?.date

    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Logbook(navBar.today.toString())) {
            composable<Logbook> { backStackEntry ->
                LogbookScreen(
                    date = LocalDate.parse(backStackEntry.toRoute<Logbook>().date),
                    contentPadding = PaddingValues(bottom = barHeight),
                )
            }
        }

        if (entry?.destination.showsNavBar()) {
            NavBar(
                days = navBar.week,
                selected = selected?.let(LocalDate::parse) ?: navBar.today,
                onSelect = { navController.navigate(Logbook(it.toString())) { launchSingleTop = true } },
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
