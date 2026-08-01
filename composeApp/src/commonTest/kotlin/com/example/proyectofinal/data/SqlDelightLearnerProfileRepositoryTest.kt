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
        assertNull(repository.getProfile("user-a"))
        assertFalse(repository.isOnboardingComplete("user-a"))
    }

    @Test
    fun `upsertProfile saves and reads back a completed technical secondary profile`() = runTest {
        val profile = LearnerProfile(
            province = "Buenos Aires",
            schoolYear = 13,
            studentTrack = StudentTrack.TECHNICAL_SECONDARY,
            onboardingComplete = true
        )

        repository.upsertProfile("user-a", profile)

        assertEquals(profile, repository.getProfile("user-a"))
        assertTrue(repository.isOnboardingComplete("user-a"))
    }

    @Test
    fun `upsertProfile replaces the existing row and preserves self-directed mapping`() = runTest {
        repository.upsertProfile(
            "user-a",
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

        repository.upsertProfile("user-a", replacement)

        assertEquals(listOf(replacement), repository.getProfile("user-a")?.let(::listOf).orEmpty())
        assertEquals(1, database.appDatabaseQueries.selectProfile("user-a").executeAsList().size)
        assertEquals(StudentTrack.SELF_DIRECTED, repository.getProfile("user-a")?.studentTrack)
    }

    @Test
    fun `isOnboardingComplete returns false for incomplete persisted profile`() = runTest {
        repository.upsertProfile(
            "user-a",
            LearnerProfile(
                province = "Mendoza",
                schoolYear = 8,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = false
            )
        )

        assertFalse(repository.isOnboardingComplete("user-a"))
    }

    @Test
    fun `different users on the same device get independent rows`() = runTest {
        val profileA = LearnerProfile(
            province = "Buenos Aires",
            schoolYear = 13,
            studentTrack = StudentTrack.TECHNICAL_SECONDARY,
            onboardingComplete = true
        )
        val profileB = LearnerProfile(
            province = "Córdoba",
            schoolYear = 6,
            studentTrack = StudentTrack.PRIMARY,
            onboardingComplete = false
        )

        repository.upsertProfile("user-a", profileA)
        repository.upsertProfile("user-b", profileB)

        assertEquals(profileA, repository.getProfile("user-a"))
        assertEquals(profileB, repository.getProfile("user-b"))
        assertTrue(repository.isOnboardingComplete("user-a"))
        assertFalse(repository.isOnboardingComplete("user-b"))
    }

    @Test
    fun `writing user A profile never flips user B onboarding state`() = runTest {
        repository.upsertProfile(
            "user-b",
            LearnerProfile(
                province = "Santa Fe",
                schoolYear = 12,
                studentTrack = StudentTrack.SELF_DIRECTED,
                onboardingComplete = true
            )
        )

        repository.upsertProfile(
            "user-a",
            LearnerProfile(
                province = "Mendoza",
                schoolYear = 8,
                studentTrack = StudentTrack.SECONDARY,
                onboardingComplete = false
            )
        )

        assertTrue(repository.isOnboardingComplete("user-b"))
        assertFalse(repository.isOnboardingComplete("user-a"))
    }

    @Test
    fun `upsertProfile replaces only within one user and keeps row count at one per user`() = runTest {
        repository.upsertProfile(
            "user-a",
            LearnerProfile(
                province = "Córdoba",
                schoolYear = 6,
                studentTrack = StudentTrack.PRIMARY,
                onboardingComplete = true
            )
        )
        repository.upsertProfile(
            "user-b",
            LearnerProfile(
                province = "Santa Fe",
                schoolYear = 12,
                studentTrack = StudentTrack.SELF_DIRECTED,
                onboardingComplete = true
            )
        )

        val replacementA = LearnerProfile(
            province = "Mendoza",
            schoolYear = 8,
            studentTrack = StudentTrack.SECONDARY,
            onboardingComplete = false
        )
        repository.upsertProfile("user-a", replacementA)

        assertEquals(replacementA, repository.getProfile("user-a"))
        assertEquals(1, database.appDatabaseQueries.selectProfile("user-a").executeAsList().size)
        assertEquals(1, database.appDatabaseQueries.selectProfile("user-b").executeAsList().size)
    }

    @Test
    fun `upsertProfile round-trips every StudentTrack displayName unchanged`() = runTest {
        StudentTrack.entries.forEachIndexed { index, track ->
            val userId = "round-trip-user-$index"
            val profile = LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 7,
                studentTrack = track,
                onboardingComplete = true
            )

            repository.upsertProfile(userId, profile)

            val persistedDisplayName = database.appDatabaseQueries
                .selectProfile(userId)
                .executeAsOne()
                .studentTrack
            assertEquals(track.displayName, persistedDisplayName)

            val reloaded = repository.getProfile(userId)
            assertEquals(track, reloaded?.studentTrack)
            assertEquals(track, StudentTrack.parse(persistedDisplayName))
        }
    }

    @Test
    fun `blank userId short-circuits without touching the database`() = runTest {
        assertNull(repository.getProfile(""))
        assertFalse(repository.isOnboardingComplete(""))

        repository.upsertProfile(
            "",
            LearnerProfile(
                province = "Buenos Aires",
                schoolYear = 13,
                studentTrack = StudentTrack.TECHNICAL_SECONDARY,
                onboardingComplete = true
            )
        )

        assertNull(repository.getProfile(""))
        assertFalse(repository.isOnboardingComplete(""))
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
            schoolYearAdapter = intAdapter,
            durationMinutesAdapter = intAdapter,
            xpRewardAdapter = intAdapter
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
