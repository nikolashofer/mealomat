package com.example.mealomat.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

@Serializable
data object Logbook

// Whitelist, so a new route is without the nav bar until added here.
fun NavDestination?.showsNavBar() = this?.hasRoute<Logbook>() == true
