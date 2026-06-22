package com.example.proyectofinal

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import com.example.proyectofinal.db.createTestDriver
import com.example.proyectofinal.di.appModule
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.ui.CourseUiState
import com.example.proyectofinal.ui.CourseViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {
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
    fun `app module resolves http client course repository and course view model`() = runTest(dispatcher) {
        val koinApp = koinApplication {
            allowOverride(true)
            modules(
                appModule,
                module {
                    single { createTestAppDatabase() }
                }
            )
        }

        try {
            val koin = koinApp.koin

            assertNotNull(koin.get<HttpClient>())
            assertNotNull(koin.get<CourseRepository>())
            assertNotNull(koin.get<CourseViewModel>())
        } finally {
            koinApp.close()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CourseViewModelTest {
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
    fun `view model transitions from loading to success`() = runTest(dispatcher) {
        val expectedCourses = listOf(
            Course(
                id = "course-1",
                title = "Fractions",
                description = "Learn fractions",
                creatorId = "teacher-1",
                isOfficial = true
            )
        )
        val emittedStates = mutableListOf<CourseUiState>()
        val viewModel = CourseViewModel(FakeCourseRepository { expectedCourses })

        val collectionJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.take(2).toList(emittedStates)
        }

        advanceUntilIdle()

        assertEquals(
            listOf(CourseUiState.Loading, CourseUiState.Success(expectedCourses)),
            emittedStates
        )
        collectionJob.cancel()
    }

    @Test
    fun `view model transitions from loading to error`() = runTest(dispatcher) {
        val emittedStates = mutableListOf<CourseUiState>()
        val viewModel = CourseViewModel(FakeCourseRepository { throw IllegalStateException("Network unavailable") })

        val collectionJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.take(2).toList(emittedStates)
        }

        advanceUntilIdle()

        assertEquals(CourseUiState.Loading, emittedStates.first())
        assertEquals(CourseUiState.Error("Network unavailable"), emittedStates.last())
        collectionJob.cancel()
    }
}

private class FakeCourseRepository(
    private val officialCoursesProvider: suspend () -> List<Course>
) : CourseRepository {
    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> = officialCoursesProvider()

    override suspend fun getCourseById(id: String): Course? = null

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = emptyList()

    override suspend fun getEnrolledCourses(userId: String): List<Course> = emptyList()

    override suspend fun createCourse(course: Course): Course = course

    override suspend fun updateCourse(course: Course): Course = course

    override suspend fun deleteCourse(id: String) = Unit

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = null
}

private fun createTestAppDatabase(): AppDatabase {
    val intAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()

        override fun encode(value: Int): Long = value.toLong()
    }

    return AppDatabase(
        driver = createTestDriver(),
        CourseEntityAdapter = CourseEntity.Adapter(
            schoolYearAdapter = intAdapter
        ),
        ExerciseEntityAdapter = ExerciseEntity.Adapter(
            typeAdapter = EnumColumnAdapter()
        ),
        UserProgressEntityAdapter = UserProgressEntity.Adapter(
            totalScoreAdapter = intAdapter
        ),
        UserEntityAdapter = UserEntity.Adapter(
            roleAdapter = userRoleColumnAdapter
        )
    )
}
