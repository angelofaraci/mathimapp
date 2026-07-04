package com.example.proyectofinal.ui.catalog

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.ExerciseCompletionResponse
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
    fun `load derives continue enroll and start CTA states`() = runTest(dispatcher) {
        val courseWithJoinCode = sampleCourse(joinCode = "JOIN-123")
        val courseWithoutJoinCode = sampleCourse(id = "course-2", joinCode = null)

        val enrolledViewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(courseWithJoinCode.id to courseWithJoinCode)),
            userRepository = FakeCourseDetailUserRepository(
                progressResponses = mutableListOf(UserProgress(testUser.id, enrolledCourseIds = setOf(courseWithJoinCode.id)))
            )
        )
        enrolledViewModel.load(courseWithJoinCode.id)
        advanceUntilIdle()
        assertEquals(CourseDetailCta.Continue, enrolledViewModel.uiState.value.cta)

        val unenrolledViewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(courseWithJoinCode.id to courseWithJoinCode)),
            userRepository = FakeCourseDetailUserRepository(
                progressResponses = mutableListOf(UserProgress(testUser.id, enrolledCourseIds = emptySet()))
            )
        )
        unenrolledViewModel.load(courseWithJoinCode.id)
        advanceUntilIdle()
        assertEquals(CourseDetailCta.Enroll, unenrolledViewModel.uiState.value.cta)

        val noJoinCodeViewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(courseWithoutJoinCode.id to courseWithoutJoinCode)),
            userRepository = FakeCourseDetailUserRepository(
                progressResponses = mutableListOf(UserProgress(testUser.id, enrolledCourseIds = emptySet()))
            )
        )
        noJoinCodeViewModel.load(courseWithoutJoinCode.id)
        advanceUntilIdle()
        assertEquals(CourseDetailCta.Start, noJoinCodeViewModel.uiState.value.cta)
    }

    @Test
    fun `load handles token only restore without crashing`() = runTest(dispatcher) {
        val course = sampleCourse(joinCode = "JOIN-123")
        val viewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(user = null),
            courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(course.id to course)),
            userRepository = FakeCourseDetailUserRepository(progressResponses = mutableListOf())
        )

        viewModel.load(course.id)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals(course, this.course)
            assertEquals(CourseDetailCta.Enroll, cta)
            assertEquals("Authenticated user not available", errorMessage)
        }
    }

    @Test
    fun `enrollment success refreshes progress and flips CTA to continue`() = runTest(dispatcher) {
        val course = sampleCourse(joinCode = "JOIN-123")
        val courseRepository = FakeCourseDetailCourseRepository(
            courses = mapOf(course.id to course),
            joinResult = course
        )
        val userRepository = FakeCourseDetailUserRepository(
            progressResponses = mutableListOf(
                UserProgress(testUser.id, enrolledCourseIds = emptySet()),
                UserProgress(testUser.id, enrolledCourseIds = setOf(course.id))
            )
        )
        val viewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = courseRepository,
            userRepository = userRepository
        )

        viewModel.load(course.id)
        advanceUntilIdle()
        viewModel.onPrimaryAction()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isSubmitting)
            assertEquals(CourseDetailCta.Continue, cta)
            assertEquals(null, errorMessage)
        }
        assertEquals(listOf(testUser.id, testUser.id), userRepository.progressRequests)
        assertEquals(2, userRepository.progressCallCount)
        assertEquals(listOf(testUser.id to "JOIN-123"), courseRepository.joinRequests)
    }

    @Test
    fun `start CTA does not call joinCourseByCode when course has no join code`() = runTest(dispatcher) {
        val course = sampleCourse(joinCode = null)
        val courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(course.id to course))
        val userRepository = FakeCourseDetailUserRepository(
            progressResponses = mutableListOf(UserProgress(testUser.id, enrolledCourseIds = emptySet()))
        )
        val viewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = courseRepository,
            userRepository = userRepository
        )

        viewModel.load(course.id)
        advanceUntilIdle()

        assertEquals(CourseDetailCta.Start, viewModel.uiState.value.cta)

        viewModel.onPrimaryAction()
        advanceUntilIdle()

        assertTrue(courseRepository.joinRequests.isEmpty())
        assertEquals(1, userRepository.progressCallCount)
        assertEquals(CourseDetailCta.Start, viewModel.uiState.value.cta)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `enrollment failure keeps CTA tappable and exposes simple error`() = runTest(dispatcher) {
        val course = sampleCourse(joinCode = "JOIN-123")
        val viewModel = CourseDetailViewModel(
            authRepository = FakeCourseDetailAuthRepository(testUser),
            courseRepository = FakeCourseDetailCourseRepository(courses = mapOf(course.id to course), joinResult = null),
            userRepository = FakeCourseDetailUserRepository(
                progressResponses = mutableListOf(UserProgress(testUser.id, enrolledCourseIds = emptySet()))
            )
        )

        viewModel.load(course.id)
        advanceUntilIdle()
        viewModel.onPrimaryAction()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertFalse(isSubmitting)
            assertEquals(CourseDetailCta.Enroll, cta)
            assertEquals("Unable to enroll in this course", errorMessage)
            assertEquals(course, this.course)
        }
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private fun sampleCourse(
    id: String = "course-1",
    joinCode: String? = "JOIN-123"
) = Course(
    id = id,
    title = "Fractions basics",
    description = "Learn fractions step by step.",
    creatorId = "teacher-1",
    isOfficial = true,
    joinCode = joinCode,
    topic = "Fractions",
    difficulty = "Beginner",
    durationMinutes = 20,
    xpReward = 50,
    schoolYear = 7
)

private class FakeCourseDetailAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))

    override val session: StateFlow<AuthSession> = state

    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)

    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)

    override fun logout() = Unit
}

private class FakeCourseDetailCourseRepository(
    private val courses: Map<String, Course>,
    private val joinResult: Course? = null
) : CourseRepository {
    val joinRequests = mutableListOf<Pair<String, String>>()

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = courses.values.toList()

    override suspend fun getCourseById(id: String): Course? = courses[id]

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? {
        joinRequests += userId to code
        return joinResult
    }
}

private class FakeCourseDetailUserRepository(
    private val progressResponses: MutableList<UserProgress>
) : UserRepository {
    val progressRequests = mutableListOf<String>()
    var progressCallCount = 0
        private set

    override suspend fun getCurrentUser(): User? = testUser

    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

    override suspend fun updateUser(user: User) = Unit

    override suspend fun getUserProgress(userId: String): UserProgress {
        progressRequests += userId
        progressCallCount += 1
        return progressResponses.removeFirstOrNull() ?: UserProgress(userId = userId)
    }

    override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse =
        error("Not used in these tests")
}
