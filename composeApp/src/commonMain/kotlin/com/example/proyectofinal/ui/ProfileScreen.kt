package com.example.proyectofinal.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.ic_arrow_left
import proyectofinal.composeapp.generated.resources.ic_bell
import proyectofinal.composeapp.generated.resources.ic_edit
import proyectofinal.composeapp.generated.resources.ic_file_text
import proyectofinal.composeapp.generated.resources.ic_flag
import proyectofinal.composeapp.generated.resources.ic_flame
import proyectofinal.composeapp.generated.resources.ic_globe
import proyectofinal.composeapp.generated.resources.ic_help_circle
import proyectofinal.composeapp.generated.resources.ic_info
import proyectofinal.composeapp.generated.resources.ic_lock
import proyectofinal.composeapp.generated.resources.ic_logout
import proyectofinal.composeapp.generated.resources.ic_mail
import proyectofinal.composeapp.generated.resources.ic_moon
import proyectofinal.composeapp.generated.resources.ic_person
import proyectofinal.composeapp.generated.resources.ic_settings
import proyectofinal.composeapp.generated.resources.ic_shield
import proyectofinal.composeapp.generated.resources.ic_volume

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
            role = uiState.role,
            streak = uiState.streak
        )
        Spacer(Modifier.size(2.dp))
        ProfileNavigationCard(
            title = "Cuenta",
            subtitle = "Nombre, correo, contraseña",
            onClick = { onDestinationSelected(ProfileSubScreen.ACCOUNT) },
            icon = { ProfileNavIcon(Res.drawable.ic_person, MaterialTheme.colorScheme.primary) }
        )
        ProfileNavigationCard(
            title = "Preferencias",
            subtitle = "Notificaciones, sonidos, idioma",
            onClick = { onDestinationSelected(ProfileSubScreen.PREFERENCES) },
            icon = { ProfileNavIcon(Res.drawable.ic_settings, MaterialTheme.colorScheme.secondary) }
        )
        ProfileNavigationCard(
            title = "Ayuda y soporte",
            subtitle = "FAQ, contacto, reportar un problema",
            onClick = { onDestinationSelected(ProfileSubScreen.HELP) },
            icon = { ProfileNavIcon(Res.drawable.ic_help_circle, MaterialTheme.colorScheme.error) }
        )
        ProfileNavigationCard(
            title = "Acerca de",
            subtitle = "Términos, privacidad, versión",
            onClick = { onDestinationSelected(ProfileSubScreen.ABOUT) },
            icon = { ProfileNavIcon(Res.drawable.ic_info, MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        Spacer(Modifier.size(10.dp))
        Surface(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppThemeDefaults.shapes.button),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_logout),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = "MathimApp · versión ${appVersionName()}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileIdentity(displayName: String, email: String, role: UserRole, streak: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(92.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.toInitials(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .semantics { contentDescription = "Editar avatar" }
                    .clickable {
                        // TODO: Open an avatar picker when platform-specific support is available.
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(displayName, style = MaterialTheme.typography.headlineSmall)
        Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChip {
                Text(
                    text = role.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (streak > 0) {
                ProfileChip(modifier = Modifier.testTag("streakChip")) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_flame),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "Racha $streak días",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileChip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileNavIcon(resource: DrawableResource, tint: Color) {
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun ProfileSubScreenScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics { contentDescription = "Volver" }
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_left),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(38.dp))
        }
        Spacer(Modifier.size(20.dp))
        content()
    }
}

@Composable
private fun AccountScreen(displayName: String, email: String, role: UserRole, onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Cuenta", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Nombre completo", value = displayName, leadingIcon = Res.drawable.ic_person, onClick = {
                // TODO: Wire account name editing in a separately scoped change.
            })
            ProfileListRow(label = "Correo electrónico", value = email, leadingIcon = Res.drawable.ic_mail, onClick = {
                // TODO: Wire account email editing in a separately scoped change.
            })
            ProfileListRow(label = "Contraseña", value = "Cambiar", leadingIcon = Res.drawable.ic_lock, onClick = {
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
            ProfileToggleRow(label = "Notificaciones", checked = true, leadingIcon = Res.drawable.ic_bell, onCheckedChange = {
                // TODO: Persist notification preferences in a separately scoped change.
            })
            ProfileToggleRow(label = "Sonidos", checked = true, leadingIcon = Res.drawable.ic_volume, onCheckedChange = {
                // TODO: Persist sound preferences in a separately scoped change.
            })
            ProfileToggleRow(label = "Modo oscuro", checked = false, leadingIcon = Res.drawable.ic_moon, onCheckedChange = {
                // TODO: Wire dark-mode switching when the theme change is scoped; visual placeholder only (no-op per spec).
            })
            ProfileListRow(label = "Idioma", value = "Español", leadingIcon = Res.drawable.ic_globe, onClick = {
                // TODO: Add language selection in a separately scoped change.
            })
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Ayuda y soporte", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Preguntas frecuentes", value = "", leadingIcon = Res.drawable.ic_help_circle, onClick = {
                // TODO: Add FAQ content in a separately scoped change.
            })
            ProfileListRow(label = "Contactar soporte", value = "", leadingIcon = Res.drawable.ic_mail, onClick = {
                // TODO: Add support contact behavior in a separately scoped change.
            })
            ProfileListRow(label = "Reportar un problema", value = "", leadingIcon = Res.drawable.ic_flag, onClick = {
                // TODO: Add issue reporting in a separately scoped change.
            })
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = "Acerca de", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(label = "Términos de uso", value = "", leadingIcon = Res.drawable.ic_file_text, onClick = {
                // TODO: Add terms content in a separately scoped change.
            })
            ProfileListRow(label = "Política de privacidad", value = "", leadingIcon = Res.drawable.ic_shield, onClick = {
                // TODO: Add privacy content in a separately scoped change.
            })
            ProfileListRow(label = "Versión", value = appVersionName(), leadingIcon = Res.drawable.ic_info, onClick = {
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
