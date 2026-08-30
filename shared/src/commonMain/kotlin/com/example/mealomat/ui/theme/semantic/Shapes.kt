package com.example.mealomat.ui.theme.semantic

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import com.example.mealomat.ui.theme.primitives.Radius

@Immutable
data class MealomatShapes(
    val button: ButtonShapes = ButtonShapes(),
    // split up later into sm/md/lg, etc...
    val field: CornerBasedShape = RoundedCornerShape(Radius.Xl2),
    val nav: CornerBasedShape = RoundedCornerShape(Radius.Xl5),
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50),
    val sheet: CornerBasedShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
)

@Immutable
data class ButtonShapes(
    val sm: CornerBasedShape = RoundedCornerShape(Radius.Md),
    val md: CornerBasedShape = RoundedCornerShape(Radius.Lg),
    val lg: CornerBasedShape = RoundedCornerShape(Radius.Xl2),
)
