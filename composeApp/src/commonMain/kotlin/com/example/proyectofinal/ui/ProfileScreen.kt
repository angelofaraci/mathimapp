package com.example.proyectofinal.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MLinearProgressIndicator
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.achievement_placeholder

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
        else -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProfileHeader(uiState.displayName, uiState.schoolYearLabel)
            LevelCard(uiState.level, uiState.currentXp, uiState.xpForNextLevel)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Streak", uiState.streak.toString(), Modifier.weight(1f))
                StatCard("Completed Lessons", uiState.completedLessons.toString(), Modifier.weight(1f))
            }
            AchievementSection(uiState.achievements)
            MButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                style = MButtonStyle.Outline
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun ProfileHeader(displayName: String, schoolYearLabel: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(displayName.toInitials(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            schoolYearLabel?.let {
                Surface(
                    shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@Composable
private fun LevelCard(level: Int, currentXp: Int, xpForNextLevel: Int) {
    MCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Level $level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            MLinearProgressIndicator(
                progress = { currentXp / xpForNextLevel.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text("$currentXp / $xpForNextLevel XP", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    MCard(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AchievementSection(achievements: List<ProfileAchievement>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Achievements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        achievements.chunked(2).forEach { rowAchievements ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowAchievements.forEach { AchievementCard(it, Modifier.weight(1f)) }
                if (rowAchievements.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: ProfileAchievement, modifier: Modifier = Modifier) {
    MCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painter = painterResource(Res.drawable.achievement_placeholder),
                contentDescription = achievement.name,
                tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(achievement.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (achievement.isUnlocked) "Unlocked" else "Locked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun String.toInitials(): String = trim().split(" ").filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifBlank { "U" }
