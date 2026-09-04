package com.example.mealomat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import mealomat.shared.generated.resources.Res
import mealomat.shared.generated.resources.icon_caret_right
import mealomat.shared.generated.resources.icon_check
import mealomat.shared.generated.resources.icon_minus
import mealomat.shared.generated.resources.icon_x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class Icon(val resource: DrawableResource) {
    Check(Res.drawable.icon_check),
    CaretRight(Res.drawable.icon_caret_right),
    Minus(Res.drawable.icon_minus),
    X(Res.drawable.icon_x),
}

@Composable
fun IconImage(
    icon: Icon,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(icon.resource),
    contentDescription = contentDescription,
    modifier = modifier,
    colorFilter = ColorFilter.tint(tint),
)
