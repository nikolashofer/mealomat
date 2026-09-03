package com.example.mealomat.feature.logbook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.mealomat.feature.logbook.model.moodMascot
import com.example.mealomat.ui.components.MascotImage
import com.example.mealomat.ui.theme.MealomatTheme
import com.example.mealomat.ui.theme.Space

val RingDiameter = 124.dp
private val RingStroke = 14.dp
private val MascotSize = 80.dp

const val ProgressMillis = 200

@Composable
fun FuelRing(percent: Int, modifier: Modifier = Modifier) {
    val colors = MealomatTheme.colors
    val track = colors.border.subtle
    val fill = colors.status.success.fill

    val sweep by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100) / 100f,
        animationSpec = tween(ProgressMillis),
    )

    Box(modifier = modifier.size(RingDiameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RingDiameter)) {
            val stroke = RingStroke.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arc = Size(size.width - stroke, size.height - stroke)

            drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke))
            if (sweep > 0f) {
                drawArc(
                    color = fill,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        MascotImage(
            mascot = moodMascot(percent),
            contentDescription = null,
            modifier = Modifier.size(MascotSize).offset(y = -Space.S2),
        )
    }
}
