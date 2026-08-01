package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.models.User
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
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
    fun `selecting a province stays on the step until Continue advances and loads province options`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")

        assertEquals(OnboardingStep.PROVINCE, viewModel.uiState.value.currentStep)
        assertEquals("Buenos Aires", viewModel.uiState.value.selectedProvince)
        assertEquals(13, viewModel.uiState.value.availableSchoolYears.last().schoolYear)
        assertNull(viewModel.uiState.value.selectedSchoolYear)

        viewModel.nextStep()

        assertEquals(OnboardingStep.SCHOOL_YEAR, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `Continue requires a valid selection at every onboarding step`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.nextStep()

        assertEquals(OnboardingStep.PROVINCE, viewModel.uiState.value.currentStep)
        assertEquals("Select a valid province", viewModel.uiState.value.errorMessage)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.nextStep()

        assertEquals(OnboardingStep.SCHOOL_YEAR, viewModel.uiState.value.currentStep)
        assertEquals("Select a valid school year", viewModel.uiState.value.errorMessage)

        viewModel.selectSchoolYear(5)
        viewModel.nextStep()
        viewModel.nextStep()

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertEquals(
            "Selected category is not available for this school year",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `category step keeps four track options while only enabling valid ones`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectSchoolYear(5)
        viewModel.nextStep()

        val trackOptions = viewModel.uiState.value.trackOptions

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertEquals(StudentTrack.entries.toList(), trackOptions.map(OnboardingTrackOption::track))
        assertTrue(trackOptions.first { it.track == StudentTrack.PRIMARY }.enabled)
        assertFalse(trackOptions.first { it.track == StudentTrack.SECONDARY }.enabled)
        assertFalse(trackOptions.first { it.track == StudentTrack.TECHNICAL_SECONDARY }.enabled)
        assertTrue(trackOptions.first { it.track == StudentTrack.SELF_DIRECTED }.enabled)
    }

    @Test
    fun `province boundary rules shift secondary start and technical extra year`() = runTest(dispatcher) {
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), FakeLearnerProfileRepository())

        viewModel.selectProvince("CABA")
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        val yearSevenOptions = viewModel.uiState.value.trackOptions.associateBy(OnboardingTrackOption::track)

        assertTrue(yearSevenOptions.getValue(StudentTrack.PRIMARY).enabled)
        assertFalse(yearSevenOptions.getValue(StudentTrack.SECONDARY).enabled)

        viewModel.selectSchoolYear(8)
        val yearEightOptions = viewModel.uiState.value.trackOptions.associateBy(OnboardingTrackOption::track)

        assertTrue(yearEightOptions.getValue(StudentTrack.SECONDARY).enabled)
        assertTrue(yearEightOptions.getValue(StudentTrack.TECHNICAL_SECONDARY).enabled)

        viewModel.selectProvince("Buenos Aires")
        viewModel.selectSchoolYear(13)
        val yearThirteenOptions = viewModel.uiState.value.trackOptions.associateBy(OnboardingTrackOption::track)

        assertTrue(yearThirteenOptions.getValue(StudentTrack.TECHNICAL_SECONDARY).enabled)
        assertFalse(yearThirteenOptions.getValue(StudentTrack.SELF_DIRECTED).enabled)
    }

    @Test
    fun `completing onboarding persists the selected learner profile`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertEquals(OnboardingStep.CONFIRMATION, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(
            LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 7,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = true
            ),
            repository.savedProfile
        )
    }

    @Test
    fun `completing onboarding without all required selections shows an error and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(
            "Complete every onboarding step before continuing",
            viewModel.uiState.value.errorMessage
        )
        assertNull(repository.savedProfile)
    }

    @Test
    fun `selecting a disabled category is rejected and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectSchoolYear(5)
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        advanceUntilIdle()

        assertEquals(OnboardingStep.CATEGORY, viewModel.uiState.value.currentStep)
        assertNull(viewModel.uiState.value.selectedTrack)
        assertEquals(
            "Selected category is not available for this school year",
            viewModel.uiState.value.errorMessage
        )
        assertNull(repository.savedProfile)
        assertNotNull(
            viewModel.uiState.value.trackOptions.firstOrNull {
                it.track == StudentTrack.SECONDARY && !it.enabled
            }
        )
    }

    @Test
    fun `completing onboarding upserts the profile under the session user id`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(testUser), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(
            LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 7,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = true
            ),
            repository.profilesByUserId[testUser.id]
        )
    }

    @Test
    fun `completing onboarding with a null session user sets an error and does not persist`() = runTest(dispatcher) {
        val repository = FakeLearnerProfileRepository()
        val viewModel = OnboardingViewModel(OnboardingFakeAuthRepository(user = null), repository)

        viewModel.selectProvince("Buenos Aires")
        viewModel.nextStep()
        viewModel.selectSchoolYear(7)
        viewModel.nextStep()
        viewModel.selectTrack(StudentTrack.SECONDARY)
        viewModel.nextStep()
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(repository.profilesByUserId.isEmpty())
    }
}

private val testUser = User(
    id = "user-1",
    name = "Test Student",
    email = "student@example.com",
    role = UserRole.STUDENT
)

private class OnboardingFakeAuthRepository(user: User?) : AuthRepository {
    private val state = MutableStateFlow(AuthSession(token = "token-123", user = user))
    override val session: StateFlow<AuthSession> = state
    override suspend fun login(email: String, password: String): Result<User> = Result.success(requireNotNull(state.value.user))
    override suspend fun register(name: String, email: String, password: String): Result<User> = Result.success(requireNotNull(state.value.user))
    override fun logout() = Unit
}

private class FakeLearnerProfileRepository : LearnerProfileRepository {
    val profilesByUserId = mutableMapOf<String, LearnerProfile>()
    var savedProfile: LearnerProfile? = null

    override suspend fun getProfile(userId: String): LearnerProfile? = profilesByUserId[userId]

    override suspend fun isOnboardingComplete(userId: String): Boolean =
        profilesByUserId[userId]?.onboardingComplete == true

    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) {
        savedProfile = profile
        profilesByUserId[userId] = profile
    }
}
