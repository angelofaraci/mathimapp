package com.example.proyectofinal.ui.primitives

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class AuthFieldIconType { Email, Lock, Visibility, VisibilityOff }

@Composable
fun AuthFieldIcon(
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
            drawRoundRect(tint, Offset(left, top), Size(width, 12.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()), style = stroke)
            drawLine(tint, Offset(left + 1.dp.toPx(), top + 1.dp.toPx()), center, strokeWidth = 1.8.dp.toPx())
            drawLine(tint, Offset(size.width - left - 1.dp.toPx(), top + 1.dp.toPx()), center, strokeWidth = 1.8.dp.toPx())
        }

        AuthFieldIconType.Lock -> {
            drawRoundRect(tint, Offset(4.dp.toPx(), 10.dp.toPx()), Size(12.dp.toPx(), 8.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
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
