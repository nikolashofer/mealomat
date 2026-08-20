package com.example.mealomat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import mealomat.shared.generated.resources.Res
import mealomat.shared.generated.resources.mascot_confident
import mealomat.shared.generated.resources.mascot_cool
import mealomat.shared.generated.resources.mascot_excited
import mealomat.shared.generated.resources.mascot_happy
import mealomat.shared.generated.resources.mascot_laugh
import mealomat.shared.generated.resources.mascot_love
import mealomat.shared.generated.resources.mascot_satisfied
import mealomat.shared.generated.resources.mascot_surprised
import mealomat.shared.generated.resources.mascot_thinking
import mealomat.shared.generated.resources.mascot_tongue
import mealomat.shared.generated.resources.mascot_wink
import mealomat.shared.generated.resources.mascot_worried
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class Mascot(val resource: DrawableResource) {
    Happy(Res.drawable.mascot_happy),
    Wink(Res.drawable.mascot_wink),
    Laugh(Res.drawable.mascot_laugh),
    Tongue(Res.drawable.mascot_tongue),
    Cool(Res.drawable.mascot_cool),
    Thinking(Res.drawable.mascot_thinking),
    Surprised(Res.drawable.mascot_surprised),
    Love(Res.drawable.mascot_love),
    Excited(Res.drawable.mascot_excited),
    Confident(Res.drawable.mascot_confident),
    Satisfied(Res.drawable.mascot_satisfied),
    Worried(Res.drawable.mascot_worried),
}

@Composable
fun MascotImage(
    mascot: Mascot,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(mascot.resource),
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = ContentScale.Fit,
)
