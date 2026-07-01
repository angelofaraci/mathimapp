package com.example.proyectofinal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AuthPrimaryColor = Color(0xFF1F2937)
private val AuthLogoBackground = Color(0xFFF3F4F6)
private val AuthLogoBorder = Color(0xFFE5E7EB)

@Composable
internal fun AuthScreenScaffold(
    formTitle: String,
    formSubtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            AuthHero()
            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = formTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AuthPrimaryColor
                )
                Text(
                    text = formSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun AuthHero() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(AuthLogoBackground, RoundedCornerShape(28.dp))
                .border(1.dp, AuthLogoBorder, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = AuthPrimaryColor
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MathApp",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = AuthPrimaryColor
            )
            Text(
                text = "Learn through play and keep moving forward every day.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun AuthPrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPrimaryColor,
            contentColor = Color.White,
            disabledContainerColor = AuthPrimaryColor.copy(alpha = 0.6f),
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        )
    ) {
        content()
    }
}

@Composable
internal fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, AuthPrimaryColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthPrimaryColor)
    ) {
        Text(text)
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuthPrimaryColor,
    focusedLabelColor = AuthPrimaryColor,
    cursorColor = AuthPrimaryColor
)
