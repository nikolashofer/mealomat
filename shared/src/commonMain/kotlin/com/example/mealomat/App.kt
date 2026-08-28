package com.example.mealomat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.AuthState
import com.example.mealomat.dev.DevBootstrap
import com.example.mealomat.feature.auth.SignInScreen
import com.example.mealomat.navigation.MealomatNavHost
import com.example.mealomat.ui.components.SplashScreen
import com.example.mealomat.ui.theme.MealomatTheme
import org.koin.compose.koinInject

@Composable
fun App() = MealomatTheme {
    val auth: AuthRepository = koinInject()
    val state by auth.state.collectAsStateWithLifecycle(AuthState.Loading)

    when (state) {
        AuthState.Loading -> SplashScreen()
        AuthState.SignedOut -> SignInScreen()
        is AuthState.SignedIn -> DevBootstrap { MealomatNavHost() }
    }
}
