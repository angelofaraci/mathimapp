package com.example.proyectofinal.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `hub renders identity navigation logout version and initials fallback`() {
        var logoutCalls = 0

        composeTestRule.setContent {
            AppTheme {
                ProfileContent(
                    uiState = profileUiState(),
                    onLogout = { logoutCalls++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Alice Student").assertExists()
        composeTestRule.onNodeWithText("alice@example.com").assertExists()
        composeTestRule.onNodeWithText("Estudiante").assertExists()
        composeTestRule.onNodeWithText("Cuenta").assertExists()
        composeTestRule.onNodeWithText("Preferencias").assertExists()
        composeTestRule.onNodeWithText("Ayuda y soporte").assertExists()
        composeTestRule.onNodeWithText("Acerca de").assertExists()
        composeTestRule.onNodeWithText("Cerrar sesión").performClick()
        composeTestRule.onNodeWithText("MathimApp · version X").assertExists()
        assertEquals(1, logoutCalls)

        composeTestRule.setContent {
            AppTheme {
                ProfileContent(
                    uiState = profileUiState(displayName = ""),
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("U").assertExists()
    }

    @Test
    fun `hub is the default destination and local back returns from account`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(
                    uiState = profileUiState(),
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("MathimApp · version X").assertExists()
        composeTestRule.onNodeWithText("Cuenta").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").assertExists()
        composeTestRule.onNodeWithContentDescription("Volver").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").assertDoesNotExist()
        composeTestRule.onNodeWithText("MathimApp · version X").assertExists()
    }

    private fun profileUiState(displayName: String = "Alice Student") = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        email = "alice@example.com",
        role = UserRole.STUDENT
    )
}
