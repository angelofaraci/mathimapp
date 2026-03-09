package com.example.proyectofinal.domain

interface UserRepository {
    suspend fun getUserProgress(userId: String): UserProgress
    suspend fun saveProgress(progress: UserProgress)
}
