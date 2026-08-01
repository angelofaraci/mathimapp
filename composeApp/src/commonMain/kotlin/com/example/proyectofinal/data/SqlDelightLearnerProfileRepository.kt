package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SqlDelightLearnerProfileRepository(
    private val database: AppDatabase
) : LearnerProfileRepository {

    override suspend fun getProfile(userId: String): LearnerProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null

        database.appDatabaseQueries.selectProfile(userId).executeAsOneOrNull()?.let { entity ->
            LearnerProfile(
                province = entity.province,
                schoolYear = entity.schoolYear.toInt(),
                studentTrack = StudentTrack.parse(entity.studentTrack)
                    ?: error("Unknown persisted student track: ${entity.studentTrack}"),
                onboardingComplete = entity.onboardingComplete
            )
        }
    }

    override suspend fun isOnboardingComplete(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false

        database.appDatabaseQueries
            .selectProfile(userId)
            .executeAsOneOrNull()
            ?.onboardingComplete == true
    }

    override suspend fun upsertProfile(userId: String, profile: LearnerProfile) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext

        database.appDatabaseQueries.upsertProfile(
            userId = userId,
            province = profile.province,
            schoolYear = profile.schoolYear.toLong(),
            studentTrack = profile.studentTrack.displayName,
            onboardingComplete = profile.onboardingComplete
        )

        Unit
    }
}
