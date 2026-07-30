package com.example.proyectofinal.ui.primitives

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.theme.BrandTrack

@Composable
fun MProgressIndicator(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeWidth = strokeWidth
    )
}

@Composable
fun MLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp)),
        color = color,
        trackColor = BrandTrack,
        strokeCap = StrokeCap.Round
    )
}
