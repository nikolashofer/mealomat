package com.example.mealomat.ui.theme.primitives

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import mealomat.shared.generated.resources.Res
import mealomat.shared.generated.resources.nunito_black
import mealomat.shared.generated.resources.nunito_bold
import mealomat.shared.generated.resources.nunito_extra_bold
import org.jetbrains.compose.resources.Font

@Composable
internal fun nunito(): FontFamily = FontFamily(
    Font(Res.font.nunito_bold, FontWeight.Bold),
    Font(Res.font.nunito_extra_bold, FontWeight.ExtraBold),
    Font(Res.font.nunito_black, FontWeight.Black),
)
