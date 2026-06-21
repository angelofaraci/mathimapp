package com.example.proyectofinal.domain

import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole

interface UserRepository {
    /**
     * Returns the currently logged-in user.
     */
    suspend fun getCurrentUser(): User?

    /**
     * Checks if the user has permission to perform admin/teacher tasks.
     */
    suspend fun getUserRole(userId: String): UserRole

    /**
     * Updates user profile information.
     */
    suspend fun updateUser(user: User)

    /**
     * Fetches and syncs the user's cumulative progress.
     */
    suspend fun getUserProgress(userId: String): UserProgress

    /**
     * Completes an exercise for the authenticated learner.
     */
    suspend fun completeExercise(exerciseId: String, score: Int = 0): ExerciseCompletionResponse
}
