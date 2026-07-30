package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.theme.BrandLock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.ic_check
import proyectofinal.composeapp.generated.resources.ic_lock
import proyectofinal.composeapp.generated.resources.ic_play

/**
 * Circular serpentine-path node for the redesign lesson map (mapa-leccion.png).
 *
 * States per the handoff: Completed = teal circle + white check, Current/Unlocked = coral circle
 * + white play (the PNG has no separate unlocked look; the next actionable node is the coral one),
 * Locked = gray (`BrandLock`) circle + white lock, non-interactive.
 *
 * Tap gating is unchanged from the previous card implementation: only Unlocked/Current nodes are
 * clickable, matching `LessonMapViewModel.selectExercise` (visual sync preserves behavior).
 */
internal const val LessonNodeSizeDp = 56

@Composable
fun LessonMapNode(
    node: LessonMapNodeUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = lessonNodePalette(node.state)
    val isEnabled = node.state == LessonNodeState.Unlocked || node.state == LessonNodeState.Current

    Box(
        modifier = modifier
            .size(LessonNodeSizeDp.dp)
            .testTag("lessonMapNode-${node.index}")
            .clip(CircleShape)
            .background(palette.container)
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(palette.icon),
            contentDescription = palette.contentDescription,
            modifier = Modifier.size(24.dp),
            tint = palette.iconTint
        )
    }
}

private class LessonNodePalette(
    val container: Color,
    val icon: DrawableResource,
    val iconTint: Color,
    val contentDescription: String
)

@Composable
private fun lessonNodePalette(state: LessonNodeState): LessonNodePalette {
    val colorScheme = MaterialTheme.colorScheme
    return when (state) {
        LessonNodeState.Completed -> LessonNodePalette(
            container = colorScheme.secondary,
            icon = Res.drawable.ic_check,
            iconTint = colorScheme.onSecondary,
            contentDescription = "Lección completada"
        )

        LessonNodeState.Current, LessonNodeState.Unlocked -> LessonNodePalette(
            container = colorScheme.primary,
            icon = Res.drawable.ic_play,
            iconTint = colorScheme.onPrimary,
            contentDescription = "Lección actual"
        )

        LessonNodeState.Locked -> LessonNodePalette(
            container = BrandLock,
            icon = Res.drawable.ic_lock,
            iconTint = colorScheme.surface,
            contentDescription = "Lección bloqueada"
        )
    }
}
