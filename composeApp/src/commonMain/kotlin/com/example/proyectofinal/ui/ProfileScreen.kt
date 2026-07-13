package com.example.proyectofinal.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.ProfileListRow
import com.example.proyectofinal.ui.primitives.ProfileNavigationCard
import com.example.proyectofinal.ui.primitives.ProfileToggleRow
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)

private enum class ProfileSubScreen {
    HUB,
    ACCOUNT,
    PREFERENCES,
    HELP,
    ABOUT
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, viewModel: ProfileViewModel = koinViewModel<ProfileViewModel>()) {
    val uiState by viewModel.uiState.collectAsState()
    ProfileContent(uiState, onLogout)
}

@Composable
internal fun ProfileContent(uiState: ProfileUiState, onLogout: () -> Unit) {
    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MProgressIndicator() }
        uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        else -> {
            var destination by remember { mutableStateOf(ProfileSubScreen.HUB) }

            BackHandler(enabled = destination != ProfileSubScreen.HUB) {
                destination = ProfileSubScreen.HUB
            }

            AnimatedContent(targetState = destination) { targetDestination ->
                when (targetDestination) {
                    ProfileSubScreen.HUB -> ProfileHub(
                        uiState = uiState,
                        onDestinationSelected = { destination = it },
                        onLogout = onLogout
                    )

                    ProfileSubScreen.ACCOUNT -> AccountScreen(
                        displayName = uiState.displayName,
                        email = uiState.email,
                        role = uiState.role,
                        onBack = { destination = ProfileSubScreen.HUB }
                    )

                    ProfileSubScreen.PREFERENCES -> PreferencesScreen(onBack = { destination = ProfileSubScreen.HUB })
                    ProfileSubScreen.HELP -> HelpScreen(onBack = { destination = ProfileSubScreen.HUB })
                    ProfileSubScreen.ABOUT -> AboutScreen(onBack = { destination = ProfileSubScreen.HUB })
                }
            }
        }
    }
}

@Composable
private fun ProfileHub(
    uiState: ProfileUiState,
    onDestinationSelected: (ProfileSubScreen) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileIdentity(
            displayName = uiState.displayName,
            email = uiState.email,
            role = uiState.role
        )
        Spacer(Modifier.size(2.dp))
        ProfileNavigationCard(
            title = "Cuenta",
            subtitle = "Nombre, correo, contraseña",
            onClick = { onDestinationSelected(ProfileSubScreen.ACCOUNT) },
            icon = { ProfileCardIcon("C") }
        )
        ProfileNavigationCard(
            title = "Preferencias",
            subtitle = "Notificaciones, sonidos, idioma",
            onClick = { onDestinationSelected(ProfileSubScreen.PREFERENCES) },
            icon = { ProfileCardIcon("P") }
        )
        ProfileNavigationCard(
            title = "Ayuda y soporte",
            subtitle = "FAQ, contacto, reportar un problema",
            onClick = { onDestinationSelected(ProfileSubScreen.HELP) },
            icon = { ProfileCardIcon("?") }
        )
        ProfileNavigationCard(
            title = "Acerca de",
            subtitle = "Términos, privacidad, versión",
            onClick = { onDestinationSelected(ProfileSubScreen.ABOUT) },
            icon = { ProfileCardIcon("i") }
        )
        Spacer(Modifier.size(10.dp))
        MButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            style = MButtonStyle.Outline
        ) {
            Text("Cerrar sesión")
        }
        Text(
            text = "MathimApp · version X",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileIdentity(displayName: String, email: String, role: UserRole) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(92.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.toInitials(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "✎",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(4.dp)
                    .semantics { contentDescription = "Editar avatar" }
                    .clickable {
                        // TODO: Open an avatar picker when platform-specific support is available.
                    },
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = role.displayName(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfileCardIcon(symbol: String) {
    Text(
        text = symbol,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ProfileSubScreenScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "←",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics { contentDescription = "Volver" }
                    .clickable(onClick = onBack)
                    .padding(8.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(20.dp))
        content()
    }
}

@Composable
private fun AccountScreen(displayName: String, email: String, role: UserRole, onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Cuenta", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Nombre completo", value = displayName, onClick = {
                // TODO: Wire account name editing in a separately scoped change.
            })
            ProfileListRow(label = "Correo electrónico", value = email, onClick = {
                // TODO: Wire account email editing in a separately scoped change.
            })
            ProfileListRow(label = "Contraseña", value = "Cambiar", onClick = {
                // TODO: Wire password changes in a separately scoped change.
            })
            Text(
                text = "Tu rol actual es ${role.displayName()}. Los cambios de cuenta requerirán confirmación por correo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            MButton(
                onClick = {
                    // TODO: Add the destructive account deletion flow in a separately scoped change.
                },
                modifier = Modifier.fillMaxWidth(),
                style = MButtonStyle.Outline
            ) {
                Text("Eliminar cuenta")
            }
            Text(
                text = "Esta acción es permanente y borra todo tu progreso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreferencesScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Preferencias", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileToggleRow(label = "Notificaciones", checked = true, onCheckedChange = {
                // TODO: Persist notification preferences in a separately scoped change.
            })
            ProfileToggleRow(label = "Sonidos", checked = true, onCheckedChange = {
                // TODO: Persist sound preferences in a separately scoped change.
            })
            ProfileListRow(label = "Idioma", value = "Español", onClick = {
                // TODO: Add language selection in a separately scoped change.
            })
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Ayuda y soporte", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Preguntas frecuentes", value = "", onClick = {
                // TODO: Add FAQ content in a separately scoped change.
            })
            ProfileListRow(label = "Contactar soporte", value = "", onClick = {
                // TODO: Add support contact behavior in a separately scoped change.
            })
            ProfileListRow(label = "Reportar un problema", value = "", onClick = {
                // TODO: Add issue reporting in a separately scoped change.
            })
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Acerca de", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Términos de uso", value = "", onClick = {
                // TODO: Add terms content in a separately scoped change.
            })
            ProfileListRow(label = "Política de privacidad", value = "", onClick = {
                // TODO: Add privacy content in a separately scoped change.
            })
            ProfileListRow(label = "Versión", value = "X", onClick = {
                // TODO: Resolve the app version from a cross-platform source in a separately scoped change.
            })
        }
    }
}

private fun UserRole.displayName(): String = when (this) {
    UserRole.ADMIN -> "Administrador"
    UserRole.TEACHER -> "Docente"
    UserRole.STUDENT -> "Estudiante"
}

private fun String.toInitials(): String = trim().split(" ").filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "U" }
