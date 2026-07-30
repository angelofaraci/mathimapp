package com.example.proyectofinal.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Render coverage for the `ui-redesign-sync` profile slice (Jul 16 handoff, Jul 21 PNG noted
 * as structurally divergent — see apply-progress). Asserts the semantics-observable token
 * changes (streak chip, 42x42 nav icon boxes, sub-screen row icons, dark-mode stub toggle)
 * and guards the behavior that must survive the visual sync (navigation, no-op toggle stub).
 */
class ProfileRedesignRenderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `hub renders streak chip 42dp nav icon boxes logout card and dynamic version`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(streak = 5), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Estudiante").assertExists()
        composeTestRule.onNodeWithTag("streakChip").assertExists()
        composeTestRule.onNodeWithText("Racha 5 días").assertExists()

        // Cards merge descendants when clickable; the tagged boxes only exist in the unmerged tree.
        val iconBoxes = composeTestRule.onAllNodesWithTag("navIconBox", useUnmergedTree = true)
        iconBoxes.assertCountEquals(4)
        iconBoxes[0]
            .assertWidthIsEqualTo(42.dp)
            .assertHeightIsEqualTo(42.dp)

        composeTestRule.onNodeWithText("Cerrar sesión").assertExists()
        composeTestRule.onNodeWithText("MathimApp · versión 1.0").assertExists()
    }

    @Test
    fun `streak chip is omitted when the user has no streak`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(streak = 0), onLogout = {})
            }
        }

        composeTestRule.onNodeWithTag("streakChip").assertDoesNotExist()
        composeTestRule.onNodeWithText("Racha 0 días").assertDoesNotExist()
        composeTestRule.onNodeWithText("Estudiante").assertExists()
    }

    @Test
    fun `account sub screen renders header bar and leading row icons`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Cuenta").performClick()

        composeTestRule.onNodeWithContentDescription("Volver").assertExists()
        composeTestRule.onNodeWithText("Cuenta").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)
        composeTestRule.onNodeWithText("Nombre completo").assertExists()
        composeTestRule.onNodeWithText("Correo electrónico").assertExists()
        composeTestRule.onNodeWithText("Contraseña").assertExists()
    }

    @Test
    fun `preferences sub screen renders dark mode stub toggle as a no-op`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Preferencias").performClick()

        composeTestRule.onNodeWithText("Modo oscuro").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(4)

        val switches = composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        )
        switches.assertCountEquals(3)
        // Third switch is the dark-mode stub: a no-op per spec, so it must stay off after a tap.
        switches[2].assertIsOff().performClick().assertIsOff()
    }

    @Test
    fun `help and about sub screens render header bars and icon rows`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Ayuda y soporte").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").assertExists()
        composeTestRule.onNodeWithText("Ayuda y soporte").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)

        composeTestRule.onNodeWithContentDescription("Volver").performClick()
        composeTestRule.onNodeWithText("Acerca de").performClick()
        composeTestRule.onNodeWithContentDescription("Volver").assertExists()
        composeTestRule.onNodeWithText("Acerca de").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)
        composeTestRule.onNodeWithText("Versión").assertExists()
    }

    private fun redesignUiState(displayName: String = "Alice Student", streak: Int = 5) = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        email = "alice@example.com",
        role = UserRole.STUDENT,
        streak = streak
    )
}
