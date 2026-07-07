package com.example.proyectofinal.ui.activities

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.UserRepository
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LessonMapViewModelTest {
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
    fun `initial load unlocks only the first exercise`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
        assertEquals("exercise-fractions-1", viewModel.uiState.value.activeNode?.exercise?.id)
    }

    @Test
    fun `completed progress unlocks the next sequential exercise`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedExerciseIds = setOf("exercise-fractions-1"),
                    enrolledCourseIds = setOf("course-fractions")
                )
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                LessonNodeState.Completed,
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
    }

    @Test
    fun `selecting a locked exercise is ignored while unlocked exercise becomes current`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-3")

        assertNull(viewModel.uiState.value.selectedExerciseId)

        viewModel.selectExercise("exercise-fractions-1")

        assertEquals("exercise-fractions-1", viewModel.uiState.value.selectedExerciseId)
        assertEquals(LessonNodeState.Current, viewModel.uiState.value.nodes.first().state)
    }

    @Test
    fun `theory action opens and dismisses the lesson theory`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress()
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.openTheory()

        assertEquals("lesson-map-fractions", viewModel.uiState.value.selectedTheoryLesson?.id)

        viewModel.dismissTheory()

        assertNull(viewModel.uiState.value.selectedTheoryLesson)
    }

    @Test
    fun `submit answer completes exercise and unlocks the next one`() = runTest(dispatcher) {
        val userRepository = FakeLessonMapUserRepository(
            progress = testProgress(),
            completionResponse = ExerciseCompletionResponse(
                exerciseId = "exercise-fractions-1",
                lessonId = "lesson-map-fractions",
                lessonCompleted = false,
                progress = UserProgress(
                    userId = testUser.id,
                    completedExerciseIds = setOf("exercise-fractions-1"),
                    totalScore = 100
                )
            )
        )
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(testUser),
            userRepository = userRepository,
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()
        viewModel.selectExercise("exercise-fractions-1")
        viewModel.selectAnswer("1/4")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(listOf("exercise-fractions-1"), userRepository.completedExerciseCalls)
        assertEquals("Exercise completed. Keep going.", viewModel.uiState.value.exerciseFeedbackMessage)
        assertEquals(
            listOf(
                LessonNodeState.Completed,
                LessonNodeState.Unlocked,
                LessonNodeState.Locked,
                LessonNodeState.Locked
            ),
            viewModel.uiState.value.nodes.map(LessonMapNodeUiModel::state)
        )
    }

    @Test
    fun `refresh restores current user when session user is missing`() = runTest(dispatcher) {
        val viewModel = LessonMapViewModel(
            authRepository = FakeLessonMapAuthRepository(user = null),
            userRepository = FakeLessonMapUserRepository(
                progress = testProgress(),
                currentUser = testUser
            ),
            lessonRepository = FakeLessonRepository(),
            exerciseRepository = FakeExerciseRepository()
        )

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("exercise-fractions-1", viewModel.uiState.value.activeNode?.exercise?.id)
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private class FakeLessonMapAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))

    override val session: StateFlow<AuthSession> = state

    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)

    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)

    override fun logout() = Unit
}

private class FakeLessonMapUserRepository(
    private val progress: UserProgress,
    private val currentUser: User? = testUser,
    private val completionResponse: ExerciseCompletionResponse? = null
) : UserRepository {
    val completedExerciseCalls = mutableListOf<String>()

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

    override suspend fun updateUser(user: User) = Unit

    override suspend fun getUserProgress(userId: String): UserProgress = progress

    override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse {
        completedExerciseCalls += exerciseId
        return completionResponse ?: error("Completion response not configured")
    }
}

private class FakeLessonRepository : LessonRepository {
    override suspend fun getLessonsByCourse(courseId: String): List<Lesson> = listOf(sampleLesson())

    override suspend fun getLessonById(id: String): Lesson? = sampleLesson()

    override suspend fun createLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateLesson(lesson: Lesson): Lesson = lesson

    override suspend fun updateTheory(lessonId: String, content: String): Lesson = sampleLesson()

    override suspend fun deleteLesson(id: String) = Unit
}

private class FakeExerciseRepository : ExerciseRepository {
    override suspend fun getExercisesByLesson(lessonId: String): List<Exercise> = sampleExercises()

    override suspend fun createExercise(exercise: Exercise): Exercise = exercise

    override suspend fun updateExercise(exercise: Exercise): Exercise = exercise

    override suspend fun deleteExercise(id: String) = Unit
}

private fun sampleLesson() = Lesson(
    id = "lesson-map-fractions",
    courseId = "course-fractions",
    creatorId = "system",
    title = "Fractions Foundations",
    theoryContent = "A fraction represents a part of a whole."
)

private fun sampleExercises() = listOf(
    Exercise(
        id = "exercise-fractions-1",
        lessonId = "lesson-map-fractions",
        question = "Which fraction shows one shaded part out of four equal parts?",
        options = listOf("1/2", "1/3", "1/4", "4/1"),
        correctAnswer = "1/4",
        type = ExerciseType.MULTIPLE_CHOICE
    ),
    Exercise(
        id = "exercise-fractions-2",
        lessonId = "lesson-map-fractions",
        question = "What is 2/4 equivalent to?",
        options = listOf("1/2", "1/6", "3/4", "2/5"),
        correctAnswer = "1/2",
        type = ExerciseType.MULTIPLE_CHOICE
    ),
    Exercise(
        id = "exercise-fractions-3",
        lessonId = "lesson-map-fractions",
        question = "Add the fractions: 1/4 + 2/4.",
        options = listOf("2/8", "3/4", "3/8", "1/2"),
        correctAnswer = "3/4",
        type = ExerciseType.MULTIPLE_CHOICE
    ),
    Exercise(
        id = "exercise-fractions-4",
        lessonId = "lesson-map-fractions",
        question = "True or false: 3/3 is equal to one whole.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        type = ExerciseType.TRUE_FALSE
    )
)

private fun testProgress(
    completedExerciseIds: Set<String> = emptySet()
) = UserProgress(
    userId = testUser.id,
    completedExerciseIds = completedExerciseIds,
    enrolledCourseIds = setOf("course-fractions")
)
