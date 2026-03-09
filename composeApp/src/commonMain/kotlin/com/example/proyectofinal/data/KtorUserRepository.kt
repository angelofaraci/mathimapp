package com.example.proyectofinal.data

import com.example.proyectofinal.domain.UserProgress
import com.example.proyectofinal.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorUserRepository(private val api: UserApi) : UserRepository {
    override suspend fun getUserProgress(userId: String): UserProgress = withContext(Dispatchers.IO) {
        api.fetchUserProgress(userId)
    }

    override suspend fun saveProgress(progress: UserProgress) = withContext(Dispatchers.IO) {
        api.saveUserProgress(progress)
    }
}
