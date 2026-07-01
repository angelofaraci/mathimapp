package com.example.proyectofinal.ui.catalog

import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.models.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CourseCatalogViewModelTest {
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
    fun `view model fetches official courses for learner school year`() = runTest(dispatcher) {
        val repository = FakeCourseCatalogRepository(sampleCourses)
        val viewModel = CourseCatalogViewModel(
            courseRepository = repository,
            learnerProfileRepository = FakeCourseCatalogLearnerProfileRepository(7)
        )

        advanceUntilIdle()

        assertEquals(listOf<Int?>(7), repository.requestedSchoolYears)
        assertIs<CourseCatalogRemoteState.Success>(viewModel.uiState.value.remoteState)
        assertEquals(sampleCourses, viewModel.uiState.value.visibleCourses)
    }

    @Test
    fun `search query filters visible courses by title`() = runTest(dispatcher) {
        val viewModel = CourseCatalogViewModel(
            courseRepository = FakeCourseCatalogRepository(sampleCourses),
            learnerProfileRepository = FakeCourseCatalogLearnerProfileRepository(7)
        )

        advanceUntilIdle()
        viewModel.updateQuery("sumas")

        assertEquals(listOf("Sumas básicas"), viewModel.uiState.value.visibleCourses.map(Course::title))
    }

    @Test
    fun `topic chip filters courses and selecting it again restores all results`() = runTest(dispatcher) {
        val viewModel = CourseCatalogViewModel(
            courseRepository = FakeCourseCatalogRepository(sampleCourses),
            learnerProfileRepository = FakeCourseCatalogLearnerProfileRepository(7)
        )

        advanceUntilIdle()
        viewModel.toggleTopic("Fracciones")

        assertEquals("Fracciones", viewModel.uiState.value.selectedTopic)
        assertEquals(
            listOf("Sumas básicas", "Fracciones equivalentes"),
            viewModel.uiState.value.visibleCourses.map(Course::title)
        )

        viewModel.toggleTopic("Fracciones")

        assertEquals(null, viewModel.uiState.value.selectedTopic)
        assertEquals(sampleCourses.map(Course::title), viewModel.uiState.value.visibleCourses.map(Course::title))
    }
}

private class FakeCourseCatalogRepository(
    private val courses: List<Course>
) : CourseRepository {
    val requestedSchoolYears = mutableListOf<Int?>()

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> {
        requestedSchoolYears += schoolYear
        return courses
    }

    override suspend fun getCourseById(id: String): Course? = null

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = null
}

private class FakeCourseCatalogLearnerProfileRepository(
    schoolYear: Int?
) : LearnerProfileRepository {
    private val profile = schoolYear?.let {
        LearnerProfile(
            province = "Buenos Aires",
            schoolYear = it,
            studentTrack = StudentTrack.SECONDARY,
            onboardingComplete = true
        )
    }

    override suspend fun getProfile(): LearnerProfile? = profile

    override suspend fun isOnboardingComplete(): Boolean = profile?.onboardingComplete == true

    override suspend fun upsertProfile(profile: LearnerProfile) = Unit
}

private val sampleCourses = listOf(
    Course(
        id = "course-1",
        title = "Sumas básicas",
        description = "Aprende a sumar fracciones paso a paso.",
        creatorId = "admin",
        isOfficial = true,
        topic = "Fracciones",
        difficulty = "Fácil",
        durationMinutes = 15,
        xpReward = 50,
        schoolYear = 7
    ),
    Course(
        id = "course-2",
        title = "Álgebra inicial",
        description = "Introducción a variables y expresiones.",
        creatorId = "admin",
        isOfficial = true,
        topic = "Álgebra",
        difficulty = "Media",
        durationMinutes = 20,
        xpReward = 70,
        schoolYear = 7
    ),
    Course(
        id = "course-3",
        title = "Fracciones equivalentes",
        description = "Identifica fracciones con el mismo valor.",
        creatorId = "admin",
        isOfficial = true,
        topic = "Fracciones",
        difficulty = "Media",
        durationMinutes = 18,
        xpReward = 60,
        schoolYear = 7
    )
)
