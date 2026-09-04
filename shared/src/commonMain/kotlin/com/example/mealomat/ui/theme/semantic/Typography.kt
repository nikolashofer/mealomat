package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mealomat.ui.theme.primitives.TypeScale
import com.example.mealomat.ui.theme.primitives.nunito

private val CapsTracking = 0.08.em

@Immutable
data class MealomatTypography(
    val display: DisplayStyles,
    val body: BodyStyles,
    val strong: BodyStyles,
    val label: LabelStyles,
)

@Immutable
data class DisplayStyles(
    val xs: TextStyle,
    val sm: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
    val xl: TextStyle,
)

@Immutable
data class BodyStyles(
    val xxs: TextStyle,
    val xs: TextStyle,
    val sm: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
)

@Immutable
data class LabelStyles(
    val xxs: TextStyle,
    val xs: TextStyle,
    val sm: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
)

@Composable
fun mealomatTypography(family: FontFamily = nunito()) = MealomatTypography(
    display = DisplayStyles(
        xs = display(family, TypeScale.T20),
        sm = display(family, TypeScale.T24),
        md = display(family, TypeScale.T32),
        lg = display(family, TypeScale.T40, (-0.03).em),
        xl = display(family, TypeScale.T48, (-0.03).em),
    ),
    body = BodyStyles(
        xxs = body(family, TypeScale.T11),
        xs = body(family, TypeScale.T13),
        sm = body(family, TypeScale.T14),
        md = body(family, TypeScale.T16),
        lg = body(family, TypeScale.T18),
    ),
    strong = BodyStyles(
        xxs = strong(family, TypeScale.T11),
        xs = strong(family, TypeScale.T13),
        sm = strong(family, TypeScale.T14),
        md = strong(family, TypeScale.T16),
        lg = strong(family, TypeScale.T18),
    ),
    label = LabelStyles(
        xxs = label(family, TypeScale.T11),
        xs = label(family, TypeScale.T13),
        sm = label(family, TypeScale.T14),
        md = label(family, TypeScale.T16),
        lg = label(family, TypeScale.T18),
    )
)

@Composable
@ReadOnlyComposable
fun TextUnit.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

fun TextStyle.caps(): TextStyle = copy(letterSpacing = CapsTracking)

private fun display(family: FontFamily, size: TextUnit, spacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Black,
    fontSize = size,
    lineHeight = size,
    letterSpacing = spacing,
)


private fun body(family: FontFamily, size: TextUnit) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    letterSpacing = 0.sp,
)

private fun strong(family: FontFamily, size: TextUnit) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.ExtraBold,
    fontSize = size,
    letterSpacing = 0.sp,
)

private fun label(family: FontFamily, size: TextUnit) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Black,
    fontSize = size,
    lineHeight = size,
    letterSpacing = 0.sp,
)