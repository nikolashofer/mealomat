package com.example.mealomat.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mealomat.ui.components.Button
import com.example.mealomat.ui.components.ButtonSize
import com.example.mealomat.ui.components.Mascot
import com.example.mealomat.ui.components.MascotImage
import com.example.mealomat.ui.components.TextField
import com.example.mealomat.ui.theme.MealomatTheme
import org.koin.compose.viewmodel.koinViewModel
import com.example.mealomat.ui.theme.Space

private const val TAGLINE = "Plan the week once. Shop, prep and eat without thinking."
private const val FOOTNOTE = "Needed once, while online. Everything after this works offline."

@Composable
fun SignInScreen(viewModel: SignInViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MealomatTheme.colors
    val typography = MealomatTheme.typography

    Column(
        modifier = Modifier.fillMaxSize().background(colors.tone.brand.fill),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.S96, start = Space.S28, end = Space.S28, bottom = Space.S28),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MascotImage(Mascot.Happy, contentDescription = null, modifier = Modifier.size(MealomatTheme.sizes.mascot.hero))
            BasicText("Mealomat", style = typography.display.lg.copy(color = colors.tone.brand.onFill))
            Spacer(Modifier.height(Space.S4))
            BasicText(
                text = TAGLINE,
                style = typography.body.md.copy(
                    color = colors.tone.brand.tint,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MealomatTheme.shapes.sheet)
                .background(colors.surface.canvas)
                .padding(start = Space.S24, end = Space.S24, top = Space.S28, bottom = Space.S32),
            verticalArrangement = Arrangement.spacedBy(MealomatTheme.spacing.formGap),
        ) {
            TextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            TextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
                secure = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            state.error?.let { ErrorBanner(it) }
            Button(
                text = if (state.isSubmitting) "Signing in..." else "Sign in",
                onClick = viewModel::submit,
                tone = colors.tone.brand,
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.Lg,
                enabled = state.canSubmit,
            )
            Spacer(Modifier.weight(1f))
            BasicText(
                text = FOOTNOTE,
                modifier = Modifier.fillMaxWidth(),
                style = typography.label.xs.copy(
                    color = colors.text.tertiary,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

// TODO: maybe move to component have general toasts which are reusable throughout app
@Composable
private fun ErrorBanner(message: String) {
    val colors = MealomatTheme.colors
    BasicText(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MealomatTheme.shapes.field)
            .background(colors.status.danger.tint)
            .padding(horizontal = Space.S16, vertical = Space.S12),
        style = MealomatTheme.typography.field.value.copy(color = colors.status.danger.text),
    )
}
