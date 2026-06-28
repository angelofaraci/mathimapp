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

    override suspend fun getProfile(): LearnerProfile? = withContext(Dispatchers.IO) {
        database.appDatabaseQueries.selectProfile().executeAsOneOrNull()?.let { entity ->
            LearnerProfile(
                province = entity.province,
                schoolYear = entity.schoolYear.toInt(),
                studentTrack = StudentTrack.parse(entity.studentTrack)
                    ?: error("Unknown persisted student track: ${entity.studentTrack}"),
                onboardingComplete = entity.onboardingComplete
            )
        }
    }

    override suspend fun isOnboardingComplete(): Boolean = withContext(Dispatchers.IO) {
        database.appDatabaseQueries
            .selectProfile()
            .executeAsOneOrNull()
            ?.onboardingComplete == true
    }

    override suspend fun upsertProfile(profile: LearnerProfile) = withContext(Dispatchers.IO) {
        database.appDatabaseQueries.upsertProfile(
            province = profile.province,
            schoolYear = profile.schoolYear.toLong(),
            studentTrack = profile.studentTrack.displayName,
            onboardingComplete = profile.onboardingComplete
        )

        Unit
    }
}
