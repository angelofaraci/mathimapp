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

        composeTestRule.onNodeWithText("Student").assertExists()
        composeTestRule.onNodeWithTag("streakChip").assertExists()
        composeTestRule.onNodeWithText("5-day streak").assertExists()

        // Cards merge descendants when clickable; the tagged boxes only exist in the unmerged tree.
        val iconBoxes = composeTestRule.onAllNodesWithTag("navIconBox", useUnmergedTree = true)
        iconBoxes.assertCountEquals(4)
        iconBoxes[0]
            .assertWidthIsEqualTo(42.dp)
            .assertHeightIsEqualTo(42.dp)

        composeTestRule.onNodeWithText("Log out").assertExists()
        composeTestRule.onNodeWithText("MathimApp · version 1.0").assertExists()
    }

    @Test
    fun `streak chip is omitted when the user has no streak`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(streak = 0), onLogout = {})
            }
        }

        composeTestRule.onNodeWithTag("streakChip").assertDoesNotExist()
        composeTestRule.onNodeWithText("0-day streak").assertDoesNotExist()
        composeTestRule.onNodeWithText("Student").assertExists()
    }

    @Test
    fun `account sub screen renders header bar and leading row icons`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Account").performClick()

        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithText("Account").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)
        composeTestRule.onNodeWithText("Full name").assertExists()
        composeTestRule.onNodeWithText("Email").assertExists()
        composeTestRule.onNodeWithText("Password").assertExists()
    }

    @Test
    fun `preferences sub screen renders dark mode stub toggle as a no-op`() {
        composeTestRule.setContent {
            AppTheme {
                ProfileContent(uiState = redesignUiState(), onLogout = {})
            }
        }

        composeTestRule.onNodeWithText("Preferences").performClick()

        composeTestRule.onNodeWithText("Dark mode").assertExists()
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

        composeTestRule.onNodeWithText("Help & support").performClick()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithText("Help & support").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("About").performClick()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
        composeTestRule.onAllNodesWithTag("rowLeadingIcon", useUnmergedTree = true).assertCountEquals(3)
        composeTestRule.onNodeWithText("Version").assertExists()
    }

    private fun redesignUiState(displayName: String = "Alice Student", streak: Int = 5) = ProfileUiState(
        isLoading = false,
        displayName = displayName,
        email = "alice@example.com",
        role = UserRole.STUDENT,
        streak = streak
    )
}
