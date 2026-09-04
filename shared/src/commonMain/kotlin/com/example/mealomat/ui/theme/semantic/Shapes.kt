package com.example.mealomat.ui.theme.semantic

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import com.example.mealomat.ui.theme.primitives.Radius

@Immutable
data class MealomatShapes(
    val control: ControlShapes = ControlShapes(),
    val surface: SurfaceShapes = SurfaceShapes(),
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50),
)

@Immutable
data class ControlShapes(
    val sm: CornerBasedShape = RoundedCornerShape(Radius.Md),
    val md: CornerBasedShape = RoundedCornerShape(Radius.Lg),
    val lg: CornerBasedShape = RoundedCornerShape(Radius.Xl2),
)

@Immutable
data class SurfaceShapes(
    // TODO: CheckBox maps onto badge shape, move it to ControlShapes.xs
    val badge: CornerBasedShape = RoundedCornerShape(Radius.Xs),
    val card: CornerBasedShape = RoundedCornerShape(Radius.Xl3),
    val frame: CornerBasedShape = RoundedCornerShape(Radius.Xl5),
    val sheet: CornerBasedShape = RoundedCornerShape(36.dp),
)

fun CornerBasedShape.topOnly(): CornerBasedShape =
    copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

fun CornerBasedShape.bottomOnly(): CornerBasedShape =
    copy(topStart = ZeroCornerSize, topEnd = ZeroCornerSize)

