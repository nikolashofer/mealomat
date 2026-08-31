package com.example.mealomat.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.mealomat.domain.MeasureLabel

@Composable
fun MeasureText(
    label: MeasureLabel,
    valueStyle: TextStyle,
    unitStyle: TextStyle = valueStyle,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = buildAnnotatedString {
            append(label.value)
            append(" ")
            withStyle(unitStyle.toSpanStyle()) { append(label.unit) }
        },
        modifier = modifier,
        style = valueStyle,
    )
}
