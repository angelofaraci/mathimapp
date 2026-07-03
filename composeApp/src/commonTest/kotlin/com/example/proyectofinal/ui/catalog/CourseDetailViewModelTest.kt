package com.example.proyectofinal.ui.catalog

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.domain.auth.SessionHydrationResult
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load derives enrolled state and completed lessons for the selected course`() = runTest(dispatcher) {
        val viewModel = CourseDetailViewModel(
            authRepository = FakeDetailAuthRepository(),
            courseRepository = FakeCourseDetailRepository(sampleDetailCourse),
            userRepository = FakeCourseDetailUserRepository(
                UserProgress(
                    userId = "student-1",
                    completedLessonIds = setOf("lesson-1", "other-course-lesson"),
                    enrolledCourseIds = setOf("course-1")
                )
            )
        )

        viewModel.load("course-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.isEnrolled)
        assertEquals(sampleDetailCourse, state.course)
        assertEquals(setOf("lesson-1"), state.completedCourseLessonIds)
        assertEquals(1, state.completedLessonsCount)
        assertEquals(2, state.totalLessons)
    }

    @Test
    fun `retry reruns the last requested course after an error`() = runTest(dispatcher) {
        val repository = FakeCourseDetailRepository(sampleDetailCourse).apply {
            courseError = IllegalStateException("Network unavailable")
        }
        val viewModel = CourseDetailViewModel(
            authRepository = FakeDetailAuthRepository(),
            courseRepository = repository,
            userRepository = FakeCourseDetailUserRepository(UserProgress(userId = "student-1"))
        )

        viewModel.load("course-1")
        advanceUntilIdle()

        assertEquals("Network unavailable", viewModel.uiState.value.errorMessage)

        repository.courseError = null
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("course-1", viewModel.uiState.value.course?.id)
    }
}

private class FakeDetailAuthRepository : AuthRepository {
    private val sessionState = MutableStateFlow(
        AuthSession(
            token = "token-123",
            user = User("student-1", "Ada", "ada@example.com", UserRole.STUDENT)
        )
    )

    override val session: StateFlow<AuthSession> = sessionState

    override suspend fun login(email: String, password: String): Result<User> = Result.failure(NotImplementedError())

    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.failure(NotImplementedError())

    override suspend fun hydrateSessionIfNeeded(): SessionHydrationResult = SessionHydrationResult.Skipped

    override fun logout() = Unit
}

private class FakeCourseDetailRepository(
    private val course: Course?
) : CourseRepository {
    var courseError: Exception? = null

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = emptyList()

    override suspend fun getCourseById(id: String): Course? {
        courseError?.let { throw it }
        return course?.takeIf { it.id == id }
    }

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = null

    override suspend fun enroll(courseId: String): UserProgress = UserProgress(userId = "student-1", enrolledCourseIds = setOf(courseId))
}

private class FakeCourseDetailUserRepository(
    private val progress: UserProgress
) : UserRepository {
    override suspend fun getCurrentUser(): User? = null

    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

    override suspend fun updateUser(user: User) = Unit

    override suspend fun getUserProgress(userId: String): UserProgress = progress

    override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse {
        return ExerciseCompletionResponse(
            exerciseId = exerciseId,
            lessonId = "lesson-1",
            lessonCompleted = false,
            progress = progress
        )
    }
}

private val sampleDetailCourse = Course(
    id = "course-1",
    title = "Fracciones básicas",
    description = "Aprende equivalencias y simplificación.",
    creatorId = "admin",
    isOfficial = true,
    difficulty = "Fácil",
    xpReward = 50,
    lessons = listOf(
        Lesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Introducción",
            theoryContent = "Contenido",
            exercises = listOf(
                Exercise(
                    id = "exercise-1",
                    lessonId = "lesson-1",
                    question = "1/2 + 1/2",
                    options = listOf("1", "2"),
                    correctAnswer = "1",
                    type = ExerciseType.MULTIPLE_CHOICE
                )
            ),
            exerciseCount = 1
        ),
        Lesson(
            id = "lesson-2",
            courseId = "course-1",
            title = "Equivalencias",
            theoryContent = "Contenido",
            exerciseCount = 3
        )
    )
)
