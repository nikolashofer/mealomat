package com.example.mealomat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mealomat.feature.home.HomeScreen
import kotlinx.serialization.Serializable

// maybe move later to routes file, when routes grow
@Serializable
data object Home

@Composable
fun MealomatNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Home) {
        composable<Home> { HomeScreen() }
    }
}
