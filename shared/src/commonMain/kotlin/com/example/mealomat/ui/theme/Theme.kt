package com.example.mealomat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.mealomat.ui.theme.semantic.MealomatColors
import com.example.mealomat.ui.theme.semantic.MealomatMotion
import com.example.mealomat.ui.theme.semantic.MealomatShadows
import com.example.mealomat.ui.theme.semantic.MealomatShapes
import com.example.mealomat.ui.theme.semantic.MealomatTypography
import com.example.mealomat.ui.theme.semantic.lightColors
import com.example.mealomat.ui.theme.semantic.mealomatTypography

val LocalMealomatColors = staticCompositionLocalOf { lightColors() }
val LocalMealomatShapes = staticCompositionLocalOf { MealomatShapes() }
val LocalMealomatShadows = staticCompositionLocalOf { MealomatShadows() }
val LocalMealomatMotion = staticCompositionLocalOf { MealomatMotion() }
val LocalMealomatTypography = staticCompositionLocalOf<MealomatTypography> {
    error("MealomatTheme { } is missing")
}

@Composable
fun MealomatTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMealomatColors provides lightColors(),
        LocalMealomatShapes provides MealomatShapes(),
        LocalMealomatShadows provides MealomatShadows(),
        LocalMealomatMotion provides MealomatMotion(),
        LocalMealomatTypography provides mealomatTypography(),
        content = content,
    )
}

object MealomatTheme {
    val colors: MealomatColors
        @Composable @ReadOnlyComposable get() = LocalMealomatColors.current
    val shapes: MealomatShapes
        @Composable @ReadOnlyComposable get() = LocalMealomatShapes.current
    val shadows: MealomatShadows
        @Composable @ReadOnlyComposable get() = LocalMealomatShadows.current
    val motion: MealomatMotion
        @Composable @ReadOnlyComposable get() = LocalMealomatMotion.current
    val typography: MealomatTypography
        @Composable @ReadOnlyComposable get() = LocalMealomatTypography.current
}
