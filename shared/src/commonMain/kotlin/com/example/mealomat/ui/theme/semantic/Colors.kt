package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.mealomat.ui.theme.primitives.Palette

@Immutable
data class MealomatColors(
    val surface: SurfaceColors,
    val text: TextColors,
    val border: BorderColors,
    val edge: EdgeColors,
    val tone: ToneScheme,
    val status: StatusScheme,
    val macro: MacroColors,
)

@Immutable
data class SurfaceColors(
    val canvas: Color,
    val raised: Color,
    val subtle: Color,
    val strong: Color,
)

@Immutable
data class TextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val faint: Color,
    val strong: Color,
    val brand: Color,
)

@Immutable
data class BorderColors(
    val subtle: Color,
    val strong: Color,
    val dashed: Color,
    val selected: Color,
)

@Immutable
data class EdgeColors(
    val subtle: Color,
    val neutral: Color,
    val selected: Color,
)

@Immutable
data class ToneColors(
    val fill: Color,
    val edge: Color,
    val border: Color,
    val tint: Color,
    val onTint: Color,
    val onFill: Color,
)

@Immutable
data class ToneScheme(
    val neutral: ToneColors,
    val strong: ToneColors,
    val brand: ToneColors,
    val pantry: ToneColors,
    val shopping: ToneColors,
    val prep: ToneColors,
    val logbook: ToneColors,
)

@Immutable
data class StatusColors(
    val fill: Color,
    val tint: Color,
    val text: Color,
    val edge: Color,
)

@Immutable
data class StatusScheme(
    val success: StatusColors,
    val warning: StatusColors,
    val danger: StatusColors,
)

// TODO: remove
@Immutable
data class MacroColors(
    val protein: Color,
    val carbs: Color,
    val fat: Color,
)

fun lightColors(): MealomatColors = MealomatColors(
    surface = SurfaceColors(
        canvas = Palette.Cream50,
        raised = Palette.White,
        subtle = Palette.Cream100,
        strong = Palette.Navy800,
    ),
    text = TextColors(
        primary = Palette.Navy800,
        secondary = Palette.Navy500,
        tertiary = Palette.Navy400,
        faint = Palette.Navy300,
        strong = Palette.White,
        brand = Palette.Teal600,
    ),
    border = BorderColors(
        subtle = Palette.Cream200,
        strong = Palette.Navy300,
        dashed = Palette.Cream400,
        selected = Palette.Teal500,
    ),
    edge = EdgeColors(
        subtle = Palette.Cream200,
        neutral = Palette.Cream400,
        selected = Palette.Teal300,
    ),
    tone = ToneScheme(
        neutral = ToneColors(
            fill = Palette.Cream100,
            edge = Palette.Cream400,
            border = Palette.Cream300,
            tint = Palette.Cream50,
            onTint = Palette.Navy800,
            onFill = Palette.Navy800,
        ),
        strong = ToneColors(
            fill = Palette.Navy700,
            edge = Palette.Navy900,
            border = Palette.Navy300,
            tint = Palette.Navy100,
            onTint = Palette.Navy700,
            onFill = Palette.White,
        ),
        brand = ToneColors(
            fill = Palette.Teal500,
            edge = Palette.Teal600,
            border = Palette.Teal300,
            tint = Palette.Teal100,
            onTint = Palette.Teal700,
            onFill = Palette.White,
        ),
        pantry = ToneColors(
            fill = Palette.Amber500,
            edge = Palette.Amber700,
            border = Palette.Amber300,
            tint = Palette.Amber100,
            onTint = Palette.Amber700,
            onFill = Palette.White,
        ),
        shopping = ToneColors(
            fill = Palette.Blue500,
            edge = Palette.Blue700,
            border = Palette.Blue300,
            tint = Palette.Blue100,
            onTint = Palette.Blue700,
            onFill = Palette.White,
        ),
        prep = ToneColors(
            fill = Palette.Purple500,
            edge = Palette.Purple700,
            border = Palette.Purple300,
            tint = Palette.Purple100,
            onTint = Palette.Purple700,
            onFill = Palette.White,
        ),
        logbook = ToneColors(
            fill = Palette.Green500,
            edge = Palette.Green700,
            border = Palette.Green300,
            tint = Palette.Green100,
            onTint = Palette.Green700,
            onFill = Palette.White,
        ),
    ),
    status = StatusScheme(
        success = StatusColors(
            fill = Palette.Green500,
            tint = Palette.Green100,
            text = Palette.Green700,
            edge = Palette.Green700,
        ),
        warning = StatusColors(
            fill = Palette.Amber500,
            tint = Palette.Amber100,
            text = Palette.Amber700,
            edge = Palette.Amber700,
        ),
        danger = StatusColors(
            fill = Palette.Red500,
            tint = Palette.Red100,
            text = Palette.Red700,
            edge = Palette.Red700,
        ),
    ),
    macro = MacroColors(
        protein = Palette.Teal500,
        carbs = Palette.Amber500,
        fat = Palette.Purple500,
    ),
)
