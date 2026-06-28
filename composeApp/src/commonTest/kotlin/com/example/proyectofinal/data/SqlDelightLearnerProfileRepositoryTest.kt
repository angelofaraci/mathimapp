package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import com.example.proyectofinal.db.createTestDriver
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.StudentTrack
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightLearnerProfileRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SqlDelightLearnerProfileRepository

    @BeforeTest
    fun setup() {
        database = createTestAppDatabase()
        repository = SqlDelightLearnerProfileRepository(database)
    }

    @Test
    fun `getProfile returns null and onboarding incomplete when profile is missing`() = runTest {
        assertNull(repository.getProfile())
        assertFalse(repository.isOnboardingComplete())
    }

    @Test
    fun `upsertProfile saves and reads back a completed technical secondary profile`() = runTest {
        val profile = LearnerProfile(
            province = "Buenos Aires",
            schoolYear = 13,
            studentTrack = StudentTrack.TECHNICAL_SECONDARY,
            onboardingComplete = true
        )

        repository.upsertProfile(profile)

        assertEquals(profile, repository.getProfile())
        assertTrue(repository.isOnboardingComplete())
    }

    @Test
    fun `upsertProfile replaces the existing row and preserves self-directed mapping`() = runTest {
        repository.upsertProfile(
            LearnerProfile(
                province = "Córdoba",
                schoolYear = 6,
                studentTrack = StudentTrack.PRIMARY,
                onboardingComplete = true
            )
        )

        val replacement = LearnerProfile(
            province = "Santa Fe",
            schoolYear = 12,
            studentTrack = StudentTrack.SELF_DIRECTED,
            onboardingComplete = true
        )

        repository.upsertProfile(replacement)

        assertEquals(listOf(replacement), repository.getProfile()?.let(::listOf).orEmpty())
        assertEquals(1, database.appDatabaseQueries.selectProfile().executeAsList().size)
        assertEquals(StudentTrack.SELF_DIRECTED, repository.getProfile()?.studentTrack)
    }

    @Test
    fun `isOnboardingComplete returns false for incomplete persisted profile`() = runTest {
        repository.upsertProfile(
            LearnerProfile(
                province = "Mendoza",
                schoolYear = 8,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = false
            )
        )

        assertFalse(repository.isOnboardingComplete())
    }
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
