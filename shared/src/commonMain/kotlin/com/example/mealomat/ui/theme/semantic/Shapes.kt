package com.example.mealomat.ui.theme.semantic

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import com.example.mealomat.ui.theme.primitives.Radius

@Immutable
data class MealomatShapes(
    val button: ButtonShapes = ButtonShapes(),
)

@Immutable
data class ButtonShapes(
    val sm: CornerBasedShape = RoundedCornerShape(Radius.Md),
    val md: CornerBasedShape = RoundedCornerShape(Radius.Lg),
    val lg: CornerBasedShape = RoundedCornerShape(Radius.Xl2),
)
