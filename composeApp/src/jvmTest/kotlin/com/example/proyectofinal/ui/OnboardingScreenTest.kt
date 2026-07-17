package com.example.proyectofinal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `province list keeps continue visible and callable in a constrained viewport`() {
        var continueCalls = 0

        composeTestRule.setContent {
            AppTheme {
                Box(modifier = Modifier.size(320.dp)) {
                    OnboardingContent(
                        state = OnboardingUiState(
                            provinces = List(30) { index -> "Province $index" },
                            selectedProvince = "Province 0"
                        ),
                        onProvinceSelected = {},
                        onSchoolYearSelected = {},
                        onTrackSelected = {},
                        onContinue = { continueCalls++ },
                        onBack = {},
                        onComplete = {},
                        onLogout = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Continue").assertIsDisplayed().performClick()

        assertEquals(1, continueCalls)
    }

    @Test
    fun `confirmation content is vertically laid out and completion remains callable`() {
        var completeCalls = 0

        composeTestRule.setContent {
            AppTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                    OnboardingContent(
                        state = OnboardingUiState(
                            currentStep = OnboardingStep.CONFIRMATION,
                            selectedProvince = "Buenos Aires",
                            selectedSchoolYear = 2,
                            availableSchoolYears = listOf(
                                SchoolYearOption("2nd year", 2, emptySet())
                            ),
                            selectedTrack = StudentTrack.PRIMARY
                        ),
                        onProvinceSelected = {},
                        onSchoolYearSelected = {},
                        onTrackSelected = {},
                        onContinue = {},
                        onBack = {},
                        onComplete = { completeCalls++ },
                        onLogout = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Province: Buenos Aires").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue to courses").assertIsDisplayed().performClick()

        assertEquals(1, completeCalls)
    }
}
