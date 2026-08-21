package com.example.mealomat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.theme.MealomatTheme

@Composable
fun SplashScreen() = Box(
    modifier = Modifier.fillMaxSize().background(MealomatTheme.colors.tone.brand.fill),
    contentAlignment = Alignment.Center,
) {
    MascotImage(Mascot.Thinking, contentDescription = null, modifier = Modifier.size(MealomatTheme.sizes.mascot.hero))
}
