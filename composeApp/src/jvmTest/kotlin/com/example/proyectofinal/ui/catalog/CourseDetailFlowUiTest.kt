package com.example.proyectofinal.ui.catalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.AuthenticatedHomeScaffold
import com.example.proyectofinal.ui.MainRouter
import com.example.proyectofinal.ui.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class CourseDetailFlowUiTest {

    @Test
    fun detailScreenRendersMetadataAndPrimaryAction() = runComposeUiTest {
        val course = sampleCourse()

        setContent {
            MaterialTheme {
                CourseDetailContent(
                    uiState = CourseDetailUiState(
                        isLoading = false,
                        course = course,
                        cta = CourseDetailCta.Enroll
                    ),
                    onBack = {},
                    onPrimaryAction = {},
                    onDismissError = {}
                )
            }
        }

        onNodeWithText(course.title).assertIsDisplayed()
        onNodeWithText("Description").assertIsDisplayed()
        onNodeWithText(course.description).assertIsDisplayed()
        onNodeWithText("Topic").assertIsDisplayed()
        onNodeWithText(course.topic!!).assertIsDisplayed()
        onNodeWithText("Difficulty").assertIsDisplayed()
        onNodeWithText(course.difficulty!!).assertIsDisplayed()
        onNodeWithText("Duration").assertIsDisplayed()
        onNodeWithText("45 min").assertIsDisplayed()
        onNodeWithText("XP reward").assertIsDisplayed()
        onNodeWithText("120").assertIsDisplayed()
        onNodeWithText("Enroll").assertIsDisplayed()
        onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun courseTapOpensDetailAndBackReturnsToCatalog() = runComposeUiTest {
        val course = sampleCourse()

        setContent {
            TestAuthenticatedHomeScaffold(course = course)
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("course-card-${course.id}").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Catálogo de cursos").assertIsDisplayed()
        onNodeWithTag("course-card-${course.id}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Back").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Back").assertIsDisplayed()
        onNodeWithText("Start").assertIsDisplayed()
        onNodeWithText("Description").assertIsDisplayed()

        onNodeWithText("Back").performClick()

        onNodeWithText("Catálogo de cursos").assertIsDisplayed()
        onNodeWithText("Buscar por nombre").assertIsDisplayed()
    }

    @Test
    fun detailScreenShowsAndDismissesVisibleErrorFromProductionViewModel() = runComposeUiTest {
        val course = sampleCourse()

        setContent {
            TestAuthenticatedHomeScaffold(
                course = course,
                userProgressErrorMessage = "Progress unavailable"
            )
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("course-card-${course.id}").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("course-card-${course.id}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Progress unavailable").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Progress unavailable").assertIsDisplayed()
        onNodeWithText("Dismiss").performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Progress unavailable").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun enrollFlowShowsVisibleContinueAfterSuccessfulJoin() = runComposeUiTest {
        val course = sampleCourse(joinCode = "JOIN-123")

        setContent {
            TestAuthenticatedHomeScaffold(
                course = course,
                joinCourseResult = course,
                progressResponses = listOf(
                    UserProgress(sampleUser().id, enrolledCourseIds = emptySet()),
                    UserProgress(sampleUser().id, enrolledCourseIds = setOf(course.id))
                )
            )
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("course-card-${course.id}").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("course-card-${course.id}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Enroll").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Enroll").assertIsDisplayed()
        onNodeWithText("Enroll").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun failedEnrollShowsErrorAndKeepsVisibleEnrollActionInteractive() = runComposeUiTest {
        val course = sampleCourse(joinCode = "JOIN-123")
        val joinAttempts = AtomicInteger(0)

        setContent {
            TestAuthenticatedHomeScaffold(
                course = course,
                joinCourseErrorMessage = "Unable to enroll in this course",
                onJoinAttempt = { joinAttempts.incrementAndGet() }
            )
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("course-card-${course.id}").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("course-card-${course.id}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Enroll").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Enroll").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Unable to enroll in this course").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Unable to enroll in this course").assertIsDisplayed()
        onNodeWithText("Enroll").assertIsDisplayed()

        onNodeWithText("Enroll").performClick()

        waitUntil(timeoutMillis = 5_000) {
            joinAttempts.get() == 2
        }
    }

    @androidx.compose.runtime.Composable
    private fun TestAuthenticatedHomeScaffold(
        course: Course,
        userProgressErrorMessage: String? = null,
        joinCourseResult: Course? = null,
        joinCourseErrorMessage: String? = null,
        progressResponses: List<UserProgress> = listOf(UserProgress(sampleUser().id)),
        onJoinAttempt: (() -> Unit)? = null
    ) {
        MaterialTheme {
            val viewModelStoreOwner = remember {
                object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                }
            }

            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                KoinApplication(application = {
                    modules(
                        testModule(
                            course = course,
                            userProgressErrorMessage = userProgressErrorMessage,
                            joinCourseResult = joinCourseResult,
                            joinCourseErrorMessage = joinCourseErrorMessage,
                            progressResponses = progressResponses,
                            onJoinAttempt = onJoinAttempt
                        )
                    )
                }) {
                    AuthenticatedHomeScaffold(
                        onLogout = {},
                        router = MainRouter(initialTab = MainTab.ACTIVITIES)
                    )
                }
            }
        }
    }

    private fun testModule(
        course: Course,
        userProgressErrorMessage: String?,
        joinCourseResult: Course?,
        joinCourseErrorMessage: String?,
        progressResponses: List<UserProgress>,
        onJoinAttempt: (() -> Unit)?
    ) = module {
        single<CourseRepository> {
            FakeCourseRepository(
                course = course,
                joinCourseResult = joinCourseResult,
                joinCourseErrorMessage = joinCourseErrorMessage,
                onJoinAttempt = onJoinAttempt
            )
        }
        single<AuthRepository> { FakeAuthRepository(sampleUser()) }
        single<UserRepository> { FakeUserRepository(sampleUser(), userProgressErrorMessage, progressResponses) }
        single<LearnerProfileRepository> { FakeLearnerProfileRepository() }
        viewModelOf(::CourseCatalogViewModel)
        viewModelOf(::CourseDetailViewModel)
    }

    private fun sampleCourse(joinCode: String? = null) = Course(
        id = "course-1",
        title = "Fractions for Beginners",
        description = "Learn the basics of fractions with guided practice.",
        creatorId = "teacher-1",
        isOfficial = true,
        joinCode = joinCode,
        topic = "Fractions",
        difficulty = "Beginner",
        durationMinutes = 45,
        xpReward = 120,
        schoolYear = 6
    )

    private fun sampleUser() = User(
        id = "student-1",
        name = "Student",
        email = "student@example.com",
        role = UserRole.STUDENT
    )

    private class FakeCourseRepository(
        private val course: Course,
        private val joinCourseResult: Course? = null,
        private val joinCourseErrorMessage: String? = null,
        private val onJoinAttempt: (() -> Unit)? = null
    ) : CourseRepository {
        override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = listOf(course)

        override suspend fun getCourseById(id: String): Course? = course.takeIf { it.id == id }

        override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

        override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

        override suspend fun createCourse(course: Course): Course = error("Not used in this test")

        override suspend fun updateCourse(course: Course): Course = error("Not used in this test")

        override suspend fun deleteCourse(id: String) = error("Not used in this test")

        override suspend fun joinCourseByCode(userId: String, code: String): Course? {
            onJoinAttempt?.invoke()
            joinCourseErrorMessage?.let { throw IllegalStateException(it) }
            return joinCourseResult
        }
    }

    private class FakeAuthRepository(user: User) : AuthRepository {
        private val mutableSession = MutableStateFlow(AuthSession(token = "token", user = user))

        override val session: StateFlow<AuthSession> = mutableSession

        override suspend fun login(email: String, password: String): Result<User> = error("Not used in this test")

        override suspend fun register(name: String, email: String, password: String): Result<User> = error("Not used in this test")

        override fun logout() {
            mutableSession.value = AuthSession()
        }
    }

    private class FakeUserRepository(
        private val user: User,
        private val userProgressErrorMessage: String?,
        progressResponses: List<UserProgress>
    ) : UserRepository {
        private val queuedProgressResponses = progressResponses.toMutableList()

        override suspend fun getCurrentUser(): User? = user

        override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

        override suspend fun updateUser(user: User) = error("Not used in this test")

        override suspend fun getUserProgress(userId: String): UserProgress {
            userProgressErrorMessage?.let { throw IllegalStateException(it) }
            return queuedProgressResponses.removeFirstOrNull() ?: UserProgress(userId = userId)
        }

        override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse =
            error("Not used in this test")
    }

    private class FakeLearnerProfileRepository : LearnerProfileRepository {
        override suspend fun getProfile(): LearnerProfile? = null

        override suspend fun isOnboardingComplete(): Boolean = true

        override suspend fun upsertProfile(profile: LearnerProfile) = error("Not used in this test")
    }
}
