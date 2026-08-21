package com.example.mealomat.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ButtonSize
import com.example.mealomat.ui.components.TextField
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.ui.theme.semantic.ToneColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.koinInject
import com.example.mealomat.ui.theme.Space

@Composable
fun HomeScreen() {
    val auth: AuthRepository = koinInject()
    val scope = rememberCoroutineScope()
    val colors = MealomatTheme.colors
    var taps by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .background(colors.surface.canvas)
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = Space.S20, vertical = Space.S16),
        verticalArrangement = Arrangement.spacedBy(Space.S20),
    ) {
        BasicText(
            text = "taps: $taps",
            style = MealomatTheme.typography.label.lg.copy(color = colors.text.primary),
        )
        Button("Click me", { taps++ }, colors.tone.brand, size = ButtonSize.Lg)
        Button(
            text = "Sign out",
            onClick = { scope.launch { auth.signOut() } },
            tone = colors.tone.strong,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Lg,
        )
    }
}