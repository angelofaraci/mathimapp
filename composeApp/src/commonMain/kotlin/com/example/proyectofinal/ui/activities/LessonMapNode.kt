package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.theme.AppThemeDefaults

@Composable
fun LessonMapNode(
    node: LessonMapNodeUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = lessonNodePalette(node.state)
    val isEnabled = node.state == LessonNodeState.Unlocked || node.state == LessonNodeState.Current

    MCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (node.state == LessonNodeState.Locked) 0.82f else 1f)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = isEnabled, onClick = onClick),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.container)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.badge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = node.index.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBadge
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = node.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onContainer
                    )
                    LessonNodeBadge(node.state)
                }

                Text(
                    text = node.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onContainer.copy(alpha = 0.82f)
                )
                Text(
                    text = node.state.helperText,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.supporting
                )
            }
        }
    }
}

@Composable
private fun LessonNodeBadge(state: LessonNodeState) {
    Surface(
        shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
        color = badgeColorFor(state)
    ) {
        Text(
            text = state.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = badgeContentColorFor(state)
        )
    }
}

private data class LessonNodePalette(
    val container: androidx.compose.ui.graphics.Color,
    val onContainer: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val badge: androidx.compose.ui.graphics.Color,
    val onBadge: androidx.compose.ui.graphics.Color,
    val supporting: androidx.compose.ui.graphics.Color
)

@Composable
private fun lessonNodePalette(state: LessonNodeState): LessonNodePalette {
    val colorScheme = MaterialTheme.colorScheme
    return when (state) {
        LessonNodeState.Completed -> LessonNodePalette(
            container = colorScheme.primaryContainer,
            onContainer = colorScheme.onPrimaryContainer,
            border = colorScheme.primary.copy(alpha = 0.3f),
            badge = colorScheme.primary,
            onBadge = colorScheme.onPrimary,
            supporting = colorScheme.primary
        )

        LessonNodeState.Current -> LessonNodePalette(
            container = colorScheme.secondaryContainer,
            onContainer = colorScheme.onSecondaryContainer,
            border = colorScheme.secondary,
            badge = colorScheme.secondary,
            onBadge = colorScheme.onSecondary,
            supporting = colorScheme.secondary
        )

        LessonNodeState.Unlocked -> LessonNodePalette(
            container = colorScheme.surface,
            onContainer = colorScheme.onSurface,
            border = colorScheme.primary,
            badge = colorScheme.primary.copy(alpha = 0.14f),
            onBadge = colorScheme.primary,
            supporting = colorScheme.primary
        )

        LessonNodeState.Locked -> LessonNodePalette(
            container = colorScheme.surfaceVariant,
            onContainer = colorScheme.onSurfaceVariant,
            border = colorScheme.outlineVariant,
            badge = colorScheme.outlineVariant,
            onBadge = colorScheme.onSurface,
            supporting = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun badgeColorFor(state: LessonNodeState) = when (state) {
    LessonNodeState.Completed -> MaterialTheme.colorScheme.primary
    LessonNodeState.Current -> MaterialTheme.colorScheme.secondary
    LessonNodeState.Unlocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    LessonNodeState.Locked -> MaterialTheme.colorScheme.outlineVariant
}

@Composable
private fun badgeContentColorFor(state: LessonNodeState) = when (state) {
    LessonNodeState.Completed -> MaterialTheme.colorScheme.onPrimary
    LessonNodeState.Current -> MaterialTheme.colorScheme.onSecondary
    LessonNodeState.Unlocked -> MaterialTheme.colorScheme.primary
    LessonNodeState.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
}

private val LessonNodeState.label: String
    get() = when (this) {
        LessonNodeState.Locked -> "Locked"
        LessonNodeState.Unlocked -> "Ready"
        LessonNodeState.Completed -> "Done"
        LessonNodeState.Current -> "Current"
    }

private val LessonNodeState.helperText: String
    get() = when (this) {
        LessonNodeState.Locked -> "Complete the previous exercise to unlock this step."
        LessonNodeState.Unlocked -> "Available now — tap to focus this exercise."
        LessonNodeState.Completed -> "Completed in your saved progress."
        LessonNodeState.Current -> "Selected as your current exercise."
    }
