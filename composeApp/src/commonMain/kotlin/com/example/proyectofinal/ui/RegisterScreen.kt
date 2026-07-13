package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel
) {
    val state by viewModel.uiState.collectAsState()
    RegisterContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onAcceptedTermsChange = viewModel::setAcceptedTerms,
        onContinue = viewModel::continueStep
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    AuthScreenScaffold(
        formTitle = "Creá tu cuenta",
        formSubtitle = "Empezá a aprender matemática a tu ritmo."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WizardStepIndicator(currentStep = state.step)

            when (state.step) {
                1 -> NameStep(state, onNameChange)
                2 -> CredentialsStep(state, onEmailChange, onPasswordChange, onTogglePasswordVisibility)
                3 -> TermsStep(state, onAcceptedTermsChange)
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MButton(
                onClick = onContinue,
                enabled = !state.isLoading && (state.step != 3 || state.acceptedTerms),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        state.isLoading -> "Creando cuenta..."
                        state.step == 3 -> "Crear cuenta"
                        else -> "Continuar"
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WizardStepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val step = index + 1
            val color = if (step <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
    Text(
        text = "Paso $currentStep de 3",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NameStep(state: RegisterUiState, onNameChange: (String) -> Unit) {
    MTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text("Nombre completo") },
        placeholder = { Text("Tu nombre") },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Name] != null,
        supportingText = state.fieldErrors[RegisterField.Name]?.let { error -> { Text(error) } },
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CredentialsStep(
    state: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit
) {
    MTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = { Text("Correo electrónico") },
        placeholder = { Text("correo@ejemplo.com") },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Email] != null,
        supportingText = state.fieldErrors[RegisterField.Email]?.let { error -> { Text(error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )

    MTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = { Text("Contraseña") },
        singleLine = true,
        isError = state.fieldErrors[RegisterField.Password] != null,
        supportingText = state.fieldErrors[RegisterField.Password]?.let { error -> { Text(error) } },
        visualTransformation = if (state.isPasswordVisible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Text(
                    text = if (state.isPasswordVisible) "Ocultar" else "Mostrar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        authStyle = true,
        modifier = Modifier.fillMaxWidth()
    )

    PasswordStrengthMeter(state.passwordStrength)
}

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val filledSegments = when (strength) {
        PasswordStrength.Empty -> 0
        PasswordStrength.Weak -> 1
        PasswordStrength.Medium -> 2
        PasswordStrength.Strong -> 3
    }
    val label = when (strength) {
        PasswordStrength.Empty -> "Usá al menos 8 caracteres y combiná tipos de caracteres."
        PasswordStrength.Weak -> "Seguridad de contraseña: baja"
        PasswordStrength.Medium -> "Seguridad de contraseña: media"
        PasswordStrength.Strong -> "Seguridad de contraseña: alta"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { index ->
                val color = if (index < filledSegments) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                ) {
                    drawRoundRect(color = color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TermsStep(state: RegisterUiState, onAcceptedTermsChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAcceptedTermsChange(!state.acceptedTerms) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.acceptedTerms,
            onCheckedChange = onAcceptedTermsChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Acepto los términos y condiciones de MathimApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    state.fieldErrors[RegisterField.Terms]?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
