package com.example.mealomat.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.example.mealomat.ui.theme.semantic.Shadow

// TODO: maybe make a modifiers package and move Pressable and Edge there
fun Modifier.edge(shadow: Shadow, shape: Shape) = edge(shadow.offsetY, shape, shadow.color)

fun Modifier.edge(depth: Dp, shape: Shape, color: Color) = drawBehind {
    translate(top = depth.toPx()) {
        drawOutline(shape.createOutline(size, layoutDirection, this), color = color)
    }
}
