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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.AvatarId
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.SupportedLanguage
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.MTextField
import com.example.proyectofinal.ui.primitives.ProfileListRow
import com.example.proyectofinal.ui.primitives.ProfileNavigationCard
import com.example.proyectofinal.ui.primitives.ProfileToggleRow
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
import proyectofinal.composeapp.generated.resources.profile_about_privacy_label
import proyectofinal.composeapp.generated.resources.profile_about_terms_label
import proyectofinal.composeapp.generated.resources.profile_about_version_label
import proyectofinal.composeapp.generated.resources.profile_account_delete_action
import proyectofinal.composeapp.generated.resources.profile_account_delete_warning
import proyectofinal.composeapp.generated.resources.profile_account_email_label
import proyectofinal.composeapp.generated.resources.profile_account_name_label
import proyectofinal.composeapp.generated.resources.profile_account_password_change_value
import proyectofinal.composeapp.generated.resources.profile_account_password_label
import proyectofinal.composeapp.generated.resources.profile_account_role_notice
import proyectofinal.composeapp.generated.resources.profile_action_logout
import proyectofinal.composeapp.generated.resources.profile_avatar_edit_description
import proyectofinal.composeapp.generated.resources.profile_back_description
import proyectofinal.composeapp.generated.resources.profile_help_contact_label
import proyectofinal.composeapp.generated.resources.profile_help_faq_label
import proyectofinal.composeapp.generated.resources.profile_help_report_label
import proyectofinal.composeapp.generated.resources.profile_nav_about_subtitle
import proyectofinal.composeapp.generated.resources.profile_nav_about_title
import proyectofinal.composeapp.generated.resources.profile_nav_account_subtitle
import proyectofinal.composeapp.generated.resources.profile_nav_account_title
import proyectofinal.composeapp.generated.resources.profile_nav_help_subtitle
import proyectofinal.composeapp.generated.resources.profile_nav_help_title
import proyectofinal.composeapp.generated.resources.profile_nav_preferences_subtitle
import proyectofinal.composeapp.generated.resources.profile_nav_preferences_title
import proyectofinal.composeapp.generated.resources.profile_preferences_dark_mode_label
import proyectofinal.composeapp.generated.resources.profile_preferences_language_label
import proyectofinal.composeapp.generated.resources.profile_preferences_language_value
import proyectofinal.composeapp.generated.resources.profile_preferences_notifications_label
import proyectofinal.composeapp.generated.resources.profile_preferences_sounds_label
import proyectofinal.composeapp.generated.resources.profile_role_admin
import proyectofinal.composeapp.generated.resources.profile_role_student
import proyectofinal.composeapp.generated.resources.profile_role_teacher
import proyectofinal.composeapp.generated.resources.profile_streak_days
import proyectofinal.composeapp.generated.resources.profile_version_label

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
    ProfileContent(
        uiState = uiState,
        onLogout = onLogout,
        onIdentityUpdated = viewModel::updateIdentity,
        onPasswordChanged = viewModel::changePassword,
        onPreferencesUpdated = viewModel::updatePreferences,
        onAvatarUpdated = viewModel::updateAvatar,
        onDeleteAccount = viewModel::deleteAccount
    )
}

@Composable
internal fun ProfileContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onIdentityUpdated: (String, String) -> Unit = { _, _ -> },
    onPasswordChanged: (String, String) -> Unit = { _, _ -> },
    onPreferencesUpdated: (ProfilePreferences) -> Unit = {},
    onAvatarUpdated: (AvatarId) -> Unit = {},
    onDeleteAccount: (String) -> Unit = {}
) {
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
                        onLogout = onLogout,
                        onAvatarUpdated = onAvatarUpdated
                    )

                    ProfileSubScreen.ACCOUNT -> AccountScreen(
                        displayName = uiState.displayName,
                        email = uiState.email,
                        role = uiState.role,
                        onBack = { destination = ProfileSubScreen.HUB },
                        onIdentityUpdated = onIdentityUpdated,
                        onPasswordChanged = onPasswordChanged,
                        onDeleteAccount = onDeleteAccount
                    )

                    ProfileSubScreen.PREFERENCES -> PreferencesScreen(
                        preferences = uiState.preferences,
                        onPreferencesUpdated = onPreferencesUpdated,
                        onBack = { destination = ProfileSubScreen.HUB }
                    )
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
    onLogout: () -> Unit,
    onAvatarUpdated: (AvatarId) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileIdentity(
            displayName = uiState.displayName,
            email = uiState.email,
            role = uiState.role,
            streak = uiState.streak,
            avatarId = uiState.preferences.avatarId,
            onAvatarUpdated = onAvatarUpdated
        )
        Spacer(Modifier.size(2.dp))
        ProfileNavigationCard(
            title = stringResource(Res.string.profile_nav_account_title),
            subtitle = stringResource(Res.string.profile_nav_account_subtitle),
            onClick = { onDestinationSelected(ProfileSubScreen.ACCOUNT) },
            icon = { ProfileNavIcon(Res.drawable.ic_person, MaterialTheme.colorScheme.primary) }
        )
        ProfileNavigationCard(
            title = stringResource(Res.string.profile_nav_preferences_title),
            subtitle = stringResource(Res.string.profile_nav_preferences_subtitle),
            onClick = { onDestinationSelected(ProfileSubScreen.PREFERENCES) },
            icon = { ProfileNavIcon(Res.drawable.ic_settings, MaterialTheme.colorScheme.secondary) }
        )
        ProfileNavigationCard(
            title = stringResource(Res.string.profile_nav_help_title),
            subtitle = stringResource(Res.string.profile_nav_help_subtitle),
            onClick = { onDestinationSelected(ProfileSubScreen.HELP) },
            icon = { ProfileNavIcon(Res.drawable.ic_help_circle, MaterialTheme.colorScheme.error) }
        )
        ProfileNavigationCard(
            title = stringResource(Res.string.profile_nav_about_title),
            subtitle = stringResource(Res.string.profile_nav_about_subtitle),
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
                    text = stringResource(Res.string.profile_action_logout),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = stringResource(Res.string.profile_version_label, appVersionName()),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileIdentity(
    displayName: String,
    email: String,
    role: UserRole,
    streak: Int,
    avatarId: AvatarId?,
    onAvatarUpdated: (AvatarId) -> Unit
) {
    val editAvatarDescription = stringResource(Res.string.profile_avatar_edit_description)
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
                    .semantics { contentDescription = editAvatarDescription }
                    .clickable {
                        onAvatarUpdated(avatarId.nextAvatar())
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
                    text = role.localizedLabel(),
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
                        text = stringResource(Res.string.profile_streak_days, streak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun AvatarId?.nextAvatar(): AvatarId {
    val current = this ?: AvatarId.AVATAR_1
    return AvatarId.entries[(current.ordinal + 1) % AvatarId.entries.size]
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
    val backDescription = stringResource(Res.string.profile_back_description)
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics { contentDescription = backDescription }
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
private fun AccountScreen(
    displayName: String,
    email: String,
    role: UserRole,
    onBack: () -> Unit,
    onIdentityUpdated: (String, String) -> Unit,
    onPasswordChanged: (String, String) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var editor by remember { mutableStateOf<AccountEditor?>(null) }
    ProfileSubScreenScaffold(title = stringResource(Res.string.profile_nav_account_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(
                label = stringResource(Res.string.profile_account_name_label),
                value = displayName,
                leadingIcon = Res.drawable.ic_person,
                onClick = { editor = AccountEditor.IDENTITY }
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_account_email_label),
                value = email,
                leadingIcon = Res.drawable.ic_mail,
                onClick = { editor = AccountEditor.IDENTITY }
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_account_password_label),
                value = stringResource(Res.string.profile_account_password_change_value),
                leadingIcon = Res.drawable.ic_lock,
                onClick = { editor = AccountEditor.PASSWORD }
            )
            Text(
                text = stringResource(Res.string.profile_account_role_notice, role.localizedLabel()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            MButton(
                onClick = {
                    editor = AccountEditor.DELETE
                },
                modifier = Modifier.fillMaxWidth(),
                style = MButtonStyle.Outline
            ) {
                Text(stringResource(Res.string.profile_account_delete_action))
            }
            Text(
                text = stringResource(Res.string.profile_account_delete_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    when (editor) {
        AccountEditor.IDENTITY -> IdentityEditor(displayName, email, onDismiss = { editor = null }, onSave = onIdentityUpdated)
        AccountEditor.PASSWORD -> PasswordEditor(onDismiss = { editor = null }, onSave = onPasswordChanged)
        AccountEditor.DELETE -> DeleteAccountConfirmation(onDismiss = { editor = null }, onConfirm = onDeleteAccount)
        null -> Unit
    }
}

private enum class AccountEditor { IDENTITY, PASSWORD, DELETE }

@Composable
private fun IdentityEditor(
    displayName: String,
    email: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(displayName) }
    var address by remember { mutableStateOf(email) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MTextField(name, { name = it }, singleLine = true, label = { Text("Name") })
                MTextField(address, { address = it }, singleLine = true, label = { Text("Email") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim(), address.trim()); onDismiss() }, enabled = name.isNotBlank() && address.contains('@')) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PasswordEditor(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MTextField(currentPassword, { currentPassword = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Current password") })
                MTextField(newPassword, { newPassword = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("New password (8 characters minimum)") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(currentPassword, newPassword); onDismiss() }, enabled = currentPassword.isNotBlank() && newPassword.length >= 8) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteAccountConfirmation(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account permanently?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action is irreversible. Enter your current password to permanently delete your account.")
                MTextField(currentPassword, { currentPassword = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("Current password") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(currentPassword); onDismiss() }, enabled = currentPassword.isNotBlank()) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PreferencesScreen(
    preferences: ProfilePreferences,
    onPreferencesUpdated: (ProfilePreferences) -> Unit,
    onBack: () -> Unit
) {
    ProfileSubScreenScaffold(title = stringResource(Res.string.profile_nav_preferences_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileToggleRow(
                label = stringResource(Res.string.profile_preferences_notifications_label),
                checked = preferences.notificationsEnabled,
                leadingIcon = Res.drawable.ic_bell,
                onCheckedChange = { onPreferencesUpdated(preferences.copy(notificationsEnabled = it)) }
            )
            ProfileToggleRow(
                label = stringResource(Res.string.profile_preferences_sounds_label),
                checked = preferences.soundsEnabled,
                leadingIcon = Res.drawable.ic_volume,
                onCheckedChange = { onPreferencesUpdated(preferences.copy(soundsEnabled = it)) }
            )
            ProfileToggleRow(
                label = stringResource(Res.string.profile_preferences_dark_mode_label),
                checked = false,
                leadingIcon = Res.drawable.ic_moon,
                onCheckedChange = {
                    // TODO: Wire dark-mode switching when the theme change is scoped; visual placeholder only (no-op per spec).
                }
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_preferences_language_label),
                value = when (preferences.language ?: SupportedLanguage.SPANISH) {
                    SupportedLanguage.SPANISH -> "Español"
                    SupportedLanguage.ENGLISH -> "English"
                },
                leadingIcon = Res.drawable.ic_globe,
                onClick = {
                    val next = if (preferences.language == SupportedLanguage.ENGLISH) SupportedLanguage.SPANISH else SupportedLanguage.ENGLISH
                    onPreferencesUpdated(preferences.copy(language = next))
                }
            )
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    var showFaq by remember { mutableStateOf(false) }
    ProfileSubScreenScaffold(title = stringResource(Res.string.profile_nav_help_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(
                label = stringResource(Res.string.profile_help_faq_label),
                value = "How to learn and track progress",
                leadingIcon = Res.drawable.ic_help_circle,
                onClick = { showFaq = true }
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_help_contact_label),
                value = "No support channel configured",
                leadingIcon = Res.drawable.ic_mail,
                enabled = false,
                onClick = {}
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_help_report_label),
                value = "No reporting destination configured",
                leadingIcon = Res.drawable.ic_flag,
                enabled = false,
                onClick = {}
            )
        }
    }
    if (showFaq) {
        AlertDialog(
            onDismissRequest = { showFaq = false },
            title = { Text("Frequently asked questions") },
            text = { Text("Complete lessons and exercises to earn progress. Your profile displays your current learning activity and preferences.") },
            confirmButton = { TextButton(onClick = { showFaq = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    ProfileSubScreenScaffold(title = stringResource(Res.string.profile_nav_about_title), onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileListRow(
                label = stringResource(Res.string.profile_about_terms_label),
                value = "Not available offline",
                leadingIcon = Res.drawable.ic_file_text,
                enabled = false,
                onClick = {}
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_about_privacy_label),
                value = "Not available offline",
                leadingIcon = Res.drawable.ic_shield,
                enabled = false,
                onClick = {}
            )
            ProfileListRow(
                label = stringResource(Res.string.profile_about_version_label),
                value = appVersionName(),
                leadingIcon = Res.drawable.ic_info,
                onClick = {
                    // TODO: Resolve the app version from a cross-platform source in a separately scoped change.
                }
            )
        }
    }
}

@Composable
private fun UserRole.localizedLabel(): String = stringResource(
    when (this) {
        UserRole.ADMIN -> Res.string.profile_role_admin
        UserRole.TEACHER -> Res.string.profile_role_teacher
        UserRole.STUDENT -> Res.string.profile_role_student
    }
)

private fun String.toInitials(): String = trim().split(" ").filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "U" }
