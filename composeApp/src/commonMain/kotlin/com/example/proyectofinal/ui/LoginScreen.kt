package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.jetbrains.compose.resources.painterResource
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MTextField
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.apple_logo
import proyectofinal.composeapp.generated.resources.google_logo

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSwitchToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onLogin = viewModel::login,
        onSwitchToRegister = onSwitchToRegister
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var showRecoveryPlaceholder by remember { mutableStateOf(false) }
    AuthScreenScaffold(
        formTitle = "Hola de nuevo",
        formSubtitle = "Iniciá sesión para seguir tu racha."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Correo electrónico") },
                placeholder = { Text("correo@ejemplo.com") },
                singleLine = true,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { error -> { Text(error) } },
                leadingIcon = {
                    AuthFieldIcon(
                        type = AuthFieldIconType.Email,
                        tint = if (state.emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        description = "Correo electrónico"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                authStyle = true,
                modifier = Modifier.fillMaxWidth()
            )

            MTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                leadingIcon = {
                    AuthFieldIcon(
                        type = AuthFieldIconType.Lock,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        description = "Contraseña"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        AuthFieldIcon(
                            type = if (state.isPasswordVisible) AuthFieldIconType.VisibilityOff else AuthFieldIconType.Visibility,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            description = if (state.isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                visualTransformation = if (state.isPasswordVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                authStyle = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "¿Olvidaste tu contraseña?",
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { showRecoveryPlaceholder = true },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            if (showRecoveryPlaceholder) {
                Text(
                    text = "La recuperación de contraseña estará disponible próximamente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MButton(
                onClick = onLogin,
                enabled = !state.isLoading && state.emailError == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isLoading) "Iniciando sesión..." else "Iniciar sesión")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "o continuá con",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialButton(
                    text = "Google",
                    logo = { androidx.compose.foundation.Image(painterResource(Res.drawable.google_logo), null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
                SocialButton(
                    text = "Apple",
                    logo = { androidx.compose.foundation.Image(painterResource(Res.drawable.apple_logo), null, Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tenés cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Registrate",
                    modifier = Modifier.clickable(enabled = !state.isLoading, onClick = onSwitchToRegister),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    logo: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    MButton(
        onClick = {},
        modifier = modifier,
        style = MButtonStyle.Social
    ) {
        logo()
        Spacer(modifier = Modifier.size(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private enum class AuthFieldIconType { Email, Lock, Visibility, VisibilityOff }

@Composable
private fun AuthFieldIcon(
    type: AuthFieldIconType,
    tint: Color,
    description: String
) {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .semantics { contentDescription = description },
        onDraw = { drawAuthFieldIcon(type, tint) }
    )
}

private fun DrawScope.drawAuthFieldIcon(type: AuthFieldIconType, tint: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx())
    val left = 2.dp.toPx()
    val top = 4.dp.toPx()
    val width = size.width - left * 2

    when (type) {
        AuthFieldIconType.Email -> {
            drawRoundRect(tint, Offset(left, top), Size(width, 12.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = stroke)
            drawLine(tint, Offset(left + 1.dp.toPx(), top + 1.dp.toPx()), center, strokeWidth = 1.8.dp.toPx())
            drawLine(tint, Offset(size.width - left - 1.dp.toPx(), top + 1.dp.toPx()), center, strokeWidth = 1.8.dp.toPx())
        }

        AuthFieldIconType.Lock -> {
            drawRoundRect(tint, Offset(4.dp.toPx(), 10.dp.toPx()), Size(12.dp.toPx(), 8.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = stroke)
            drawArc(tint, 180f, 180f, false, Offset(6.dp.toPx(), 3.dp.toPx()), Size(8.dp.toPx(), 10.dp.toPx()), style = stroke)
        }

        AuthFieldIconType.Visibility,
        AuthFieldIconType.VisibilityOff -> {
            drawOval(tint, Offset(1.dp.toPx(), 6.dp.toPx()), Size(18.dp.toPx(), 8.dp.toPx()), style = stroke)
            drawCircle(tint, radius = 2.5.dp.toPx(), center = center, style = stroke)
            if (type == AuthFieldIconType.VisibilityOff) {
                drawLine(tint, Offset(3.dp.toPx(), 3.dp.toPx()), Offset(17.dp.toPx(), 17.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
        }
    }
}
