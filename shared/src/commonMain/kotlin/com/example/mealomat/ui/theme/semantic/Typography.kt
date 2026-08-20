package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mealomat.ui.theme.primitives.nunito

// INCOMPLETE. Only the kinds the built screens actually use live here; headings, body, etc...
@Immutable
data class MealomatTypography(
    val label: LabelStyles,
)

@Immutable
data class LabelStyles(
    val xs: TextStyle,
    val sm: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
)

@Composable
fun mealomatTypography(family: FontFamily = nunito()) = MealomatTypography(
    label = LabelStyles(
        xs = label(family, 13),
        sm = label(family, 14),
        md = label(family, 16),
        lg = label(family, 17),
    ),
)

private fun label(family: FontFamily, size: Int) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Black,
    fontSize = size.sp,
    letterSpacing = 0.sp,
)
