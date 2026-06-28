package com.example.proyectofinal.domain

data class LearnerProfile(
    val province: String,
    val schoolYear: Int,
    val studentTrack: StudentTrack,
    val onboardingComplete: Boolean
)

interface LearnerProfileRepository {
    suspend fun getProfile(): LearnerProfile?

    suspend fun isOnboardingComplete(): Boolean

    suspend fun upsertProfile(profile: LearnerProfile)
}
