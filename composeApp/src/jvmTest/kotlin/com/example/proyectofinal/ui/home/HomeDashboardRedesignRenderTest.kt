package com.example.proyectofinal.ui.home

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.proyectofinal.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Render coverage for the `ui-redesign-sync` home slice (inicio-dashboard.png, resolved plan in
 * Engram #69). Asserts the semantics-observable redesign pieces (greeting + wave, coral streak
 * pill omitted at 0, Nivel/XP card with teal 8dp bar, "MIS CURSOS EN PROGRESO" section, course
 * cards with "Ir" pills) and guards the behavior that must survive the visual sync (Ir pill and
 * catalog CTA open the lesson map, ContinueLearningCard flow when enrolled without progress).
 *
 * Spec-number note: "340/500 XP" at level 5 is a mock literal unreachable with XpPerLevel=100
 * (currentXp ∈ [0,99]); the values are passed straight to the content composable here to honor
 * the 68% bar scenario. Real zero progress renders "0 / 100 XP".
 */
class HomeDashboardRedesignRenderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `greeting row renders wave subtitle and coral streak pill`() {
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(greeting = "Hola, María", streak = 7),
                    onContinueLearning = {},
                    onOpenLessonMap = {},
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hola, María 👋").assertExists()
        composeTestRule.onNodeWithText("It's time to practice today!").assertExists()
        composeTestRule.onNodeWithTag("homeStreakPill").assertExists()
        composeTestRule.onNodeWithText("+7 days").assertExists()
    }

    @Test
    fun `streak pill is omitted when the user has no streak`() {
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(streak = 0),
                    onContinueLearning = {},
                    onOpenLessonMap = {},
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("homeStreakPill").assertDoesNotExist()
        composeTestRule.onNodeWithText("+0 days").assertDoesNotExist()
    }

    @Test
    fun `progress card renders level XP text and a 68 percent bar`() {
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(level = 5, currentXp = 340, xpForNextLevel = 500),
                    onContinueLearning = {},
                    onOpenLessonMap = {},
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Level 5").assertExists()
        composeTestRule.onNodeWithText("340 / 500 XP").assertExists()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(340f / 500f, 0f..1f, 0)
            )
        ).assertExists()
    }

    @Test
    fun `zero progress renders nivel 0 and an empty bar`() {
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(level = 0, currentXp = 0, xpForNextLevel = 100),
                    onContinueLearning = {},
                    onOpenLessonMap = {},
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Level 0").assertExists()
        composeTestRule.onNodeWithText("0 / 100 XP").assertExists()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0f / 100f, 0f..1f, 0)
            )
        ).assertExists()
    }

    @Test
    fun `courses section renders header course cards and ir pill opens the lesson map`() {
        var lessonMapOpens = 0
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(
                        inProgressCourses = listOf(
                            HomeCourseProgress("course-1", "Fracciones - Básico", 45),
                            HomeCourseProgress("course-2", "Álgebra Inicial", 12)
                        )
                    ),
                    onContinueLearning = {},
                    onOpenLessonMap = { lessonMapOpens++ },
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("MY COURSES IN PROGRESS").assertExists()
        composeTestRule.onNodeWithText("Fracciones - Básico").assertExists()
        composeTestRule.onNodeWithText("Progress: 45%").assertExists()
        composeTestRule.onNodeWithText("Álgebra Inicial").assertExists()
        composeTestRule.onNodeWithText("Progress: 12%").assertExists()

        composeTestRule.onAllNodesWithText("Go").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Go")[0].performClick()
        assert(lessonMapOpens == 1)

        composeTestRule.onNodeWithText("Open lesson map").performClick()
        assert(lessonMapOpens == 2)
    }

    @Test
    fun `enrolled dashboard without in-progress courses keeps the continue learning card`() {
        var continueLearningClicks = 0
        var lessonMapOpens = 0
        composeTestRule.setContent {
            AppTheme {
                HomeDashboardContent(
                    uiState = redesignUiState(inProgressCourses = emptyList()),
                    onContinueLearning = { continueLearningClicks++ },
                    onOpenLessonMap = { lessonMapOpens++ },
                    onJoinCourse = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("MY COURSES IN PROGRESS").assertDoesNotExist()
        composeTestRule.onNodeWithText("Continue learning").assertExists()
        composeTestRule.onNodeWithText("You don't have an activity in progress yet").assertExists()

        composeTestRule.onNodeWithText("Go to map").performClick()
        assert(continueLearningClicks == 1)

        composeTestRule.onNodeWithText("Open lesson map").performClick()
        assert(lessonMapOpens == 1)
    }

    private fun redesignUiState(
        greeting: String = "Buenos días, Alice Student",
        streak: Int = 5,
        level: Int = 3,
        currentXp: Int = 50,
        xpForNextLevel: Int = 100,
        inProgressCourses: List<HomeCourseProgress> = listOf(
            HomeCourseProgress("course-1", "Fracciones - Básico", 45)
        )
    ) = HomeDashboardUiState(
        isLoading = false,
        greeting = greeting,
        level = level,
        streak = streak,
        currentXp = currentXp,
        xpForNextLevel = xpForNextLevel,
        hasEnrolledCourse = true,
        inProgressCourses = inProgressCourses
    )
}
