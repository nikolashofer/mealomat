package com.example.mealomat.ui.theme.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.example.mealomat.ui.theme.Space

// INCOMPLETE. Only spacing relationships tat should stay consistent go into here
@Immutable
data class MealomatSpacing(
    val formGap: Dp = Space.S12,
    val buttonGap: Dp = Space.S12,
)
