package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mealomat.ui.theme.primitives.nunito

// INCOMPLETE. Only the kinds the built screens actually use live here; headings, body, etc...
@Immutable
data class MealomatTypography(
    val display: TextStyle,
    val body: BodyStyles,
    val label: LabelStyles,
    val number: NumberStyles,
    val field: FieldStyles,
)

@Immutable
data class BodyStyles(
    val md: TextStyle,
)

@Immutable
data class LabelStyles(
    val xs: TextStyle,
    val sm: TextStyle,
    val md: TextStyle,
    val lg: TextStyle,
    val soft: TextStyle,
    val caps: TextStyle,
)

@Immutable
data class NumberStyles(
    val lg: TextStyle,
    val unit: TextStyle,
)

@Immutable
data class FieldStyles(
    val label: TextStyle,
    val value: TextStyle,
)

@Composable
fun mealomatTypography(family: FontFamily = nunito()) = MealomatTypography(
    label = LabelStyles(
        xs = label(family, 13),
        sm = label(family, 14),
        md = label(family, 16),
        lg = label(family, 17),
        // TODO: move to body, i guess shouldnt be label lets see
        soft = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
        caps = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 0.1.em,
        ),
    ),
    number = NumberStyles(
        lg = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Black,
            fontSize = 48.sp,
            letterSpacing = (-0.03).em,
            lineHeight = 48.sp,
        ),
        unit = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 15.sp),
    ),
    display = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
    ),
    body = BodyStyles(
        md = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
    ),
    field = FieldStyles(
        label = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.08.em,
        ),
        value = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        ),
    ),
)

private fun label(family: FontFamily, size: Int) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Black,
    fontSize = size.sp,
    letterSpacing = 0.sp,
)
