package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.UpdateAvatarRequest
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.SupportedLanguage
import com.example.proyectofinal.ui.localization.AppLanguage
import com.example.proyectofinal.ui.localization.AppLocaleController
import com.example.proyectofinal.ui.localization.LocaleActivationPolicy
import com.example.proyectofinal.ui.localization.PlatformLocaleActivator
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
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `view model derives profile metrics from user progress`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = (1..12).map { "lesson-$it" }.toSet(),
                    totalScore = 350
                )
            ),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository(),
            localeController = testLocaleController()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Alice Student", displayName)
            assertEquals("alice@example.com", email)
            assertEquals(UserRole.STUDENT, role)
            assertEquals(7, schoolYear)
            assertEquals(StudentTrack.SECONDARY, studentTrack)
            assertEquals(3, level)
            assertEquals(50, currentXp)
            assertEquals(100, xpForNextLevel)
            assertEquals(7, streak)
            assertEquals(12, completedLessons)
            assertEquals(listOf(true, true, true, true), achievements.map { it.isUnlocked })
        }
    }

    @Test
    fun `view model keeps below-cap streak and locked achievements when thresholds are not met`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(
                progress = UserProgress(
                    userId = testUser.id,
                    completedLessonIds = setOf("lesson-1", "lesson-2", "lesson-3"),
                    totalScore = 0
                )
            ),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository(),
            localeController = testLocaleController()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertEquals(0, level)
            assertEquals(0, currentXp)
            assertEquals(3, streak)
            assertEquals(3, completedLessons)
            assertEquals(listOf(true, false, false, false), achievements.map { it.isUnlocked })
        }
    }

    @Test
    fun `view model exposes error message when repositories fail`() = runTest(dispatcher) {
        val viewModel = ProfileViewModel(
            authRepository = ProfileFakeAuthRepository(testUser),
            userRepository = FakeUserRepository(errorMessage = "Progress unavailable"),
            learnerProfileRepository = ProfileFakeLearnerProfileRepository(),
            localeController = testLocaleController()
        )

        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Alice Student", displayName)
            assertEquals("", email)
            assertEquals(UserRole.STUDENT, role)
            assertEquals("Progress unavailable", errorMessage)
            assertTrue(achievements.isEmpty())
        }
    }

    @Test
    fun `view model persists identity preferences avatar and confirmed deletion`() = runTest(dispatcher) {
        val repository = FakeUserRepository(
            progress = UserProgress(userId = testUser.id),
            preferences = ProfilePreferences()
        )
        val authRepository = ProfileFakeAuthRepository(testUser)
        val viewModel = ProfileViewModel(
            authRepository,
            repository,
            ProfileFakeLearnerProfileRepository(),
            testLocaleController()
        )
        advanceUntilIdle()

        viewModel.updateIdentity("Renamed", "renamed@example.com")
        viewModel.updatePreferences(ProfilePreferences(notificationsEnabled = false))
        viewModel.updateAvatar(com.example.proyectofinal.models.AvatarId.AVATAR_3)
        viewModel.deleteAccount("CurrentPassword123!")
        advanceUntilIdle()

        assertEquals("Renamed", viewModel.uiState.value.displayName)
        assertEquals("renamed@example.com", viewModel.uiState.value.email)
        assertFalse(viewModel.uiState.value.preferences.notificationsEnabled)
        assertEquals(com.example.proyectofinal.models.AvatarId.AVATAR_3, viewModel.uiState.value.preferences.avatarId)
        assertTrue(viewModel.uiState.value.accountDeleted)
        assertEquals("CurrentPassword123!", repository.deletedWithPassword)
        assertEquals(AuthSession(), authRepository.session.value)
    }

    @Test
    fun `view model applies persisted language to the authenticated account locale`() = runTest(dispatcher) {
        val localeController = testLocaleController()
        val viewModel = ProfileViewModel(
            ProfileFakeAuthRepository(testUser),
            FakeUserRepository(progress = UserProgress(userId = testUser.id)),
            ProfileFakeLearnerProfileRepository(),
            localeController
        )
        advanceUntilIdle()

        viewModel.updateLanguage(SupportedLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(AppLanguage.ENGLISH, localeController.state.value.language)
        assertEquals(SupportedLanguage.ENGLISH, viewModel.uiState.value.preferences.language)
    }

    @Test
    fun `failed account deletion preserves the authenticated session`() = runTest(dispatcher) {
        val authRepository = ProfileFakeAuthRepository(testUser)
        val viewModel = ProfileViewModel(
            authRepository,
            FakeUserRepository(
                progress = UserProgress(userId = testUser.id),
                deleteErrorMessage = "Invalid password"
            ),
            ProfileFakeLearnerProfileRepository(),
            testLocaleController()
        )
        advanceUntilIdle()

        viewModel.deleteAccount("incorrect")
        advanceUntilIdle()

        assertEquals("token-123", authRepository.session.value.token)
        assertFalse(viewModel.uiState.value.accountDeleted)
        assertEquals("Invalid password", viewModel.uiState.value.errorMessage)
    }
}

private val testUser = User(
    id = "user-1",
    name = "Alice Student",
    email = "alice@example.com",
    role = UserRole.STUDENT
)

private class ProfileFakeAuthRepository(user: User) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))
    override val session: StateFlow<AuthSession> = state
    override suspend fun login(email: String, password: String): Result<User> = Result.success(testUser)
    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(testUser)
    override fun replaceSessionUser(user: User, expectedToken: String?) { state.value = state.value.copy(user = user) }
    override fun logout() { state.value = AuthSession() }
}

private class FakeUserRepository(
    private val progress: UserProgress? = null,
    private val errorMessage: String? = null,
    private val deleteErrorMessage: String? = null,
    private var preferences: ProfilePreferences = ProfilePreferences()
) : UserRepository {
    var deletedWithPassword: String? = null
    override suspend fun getCurrentUser(): User? = testUser
    override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT
    override suspend fun updateUser(user: User) = Unit
    override suspend fun updateIdentity(request: UpdateIdentityRequest): User = testUser.copy(name = request.name, email = request.email)
    override suspend fun changePassword(request: ChangePasswordRequest) = error("Not used")
    override suspend fun deleteAccount(request: com.example.proyectofinal.models.DeleteAccountRequest) {
        deleteErrorMessage?.let { error(it) }
        deletedWithPassword = request.currentPassword
    }
    override suspend fun getProfilePreferences(): ProfilePreferences = preferences
    override suspend fun updateProfilePreferences(preferences: ProfilePreferences): ProfilePreferences = preferences.also { this.preferences = it }
    override suspend fun updateAvatar(request: UpdateAvatarRequest): ProfilePreferences = preferences.copy(avatarId = request.avatarId).also { preferences = it }
    override suspend fun getUserProgress(userId: String): UserProgress {
        errorMessage?.let { throw IllegalStateException(it) }
        return requireNotNull(progress)
    }

    override suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int
    ): ExerciseAttemptResponse = error("Not used in these tests")
}

private fun testLocaleController() = AppLocaleController(
    object : PlatformLocaleActivator {
        override val activationPolicy = LocaleActivationPolicy.IMMEDIATE
        override fun activate(accountId: String, language: AppLanguage?) = Unit
    }
)

private class ProfileFakeLearnerProfileRepository : LearnerProfileRepository {
    override suspend fun getProfile(userId: String): LearnerProfile = LearnerProfile("Buenos Aires", 7, StudentTrack.SECONDARY, true)
    override suspend fun isOnboardingComplete(userId: String): Boolean = true
    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) = Unit
}
