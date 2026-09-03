package com.example.mealomat.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

@Serializable
data object Logbook

@Serializable
data class Shopping(val blockId: String, val date: String)

// Whitelist, so a new route is without the nav bar until added here.
fun NavDestination?.showsNavBar() = this?.hasRoute<Logbook>() == true
