package com.example.proyectofinal.domain

data class LearnerProfile(
    val province: String,
    val schoolYear: Int,
    val studentTrack: StudentTrack,
    val onboardingComplete: Boolean
)

interface LearnerProfileRepository {
    suspend fun getProfile(userId: String): LearnerProfile?

    suspend fun isOnboardingComplete(userId: String): Boolean

    suspend fun upsertProfile(userId: String, profile: LearnerProfile)
}
