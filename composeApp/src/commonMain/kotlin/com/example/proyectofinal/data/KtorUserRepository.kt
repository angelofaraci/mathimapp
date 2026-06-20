package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorUserRepository(
    private val api: UserApi,
    private val database: AppDatabase
) : UserRepository {

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        try {
            val remote = api.fetchUser("current-user-id")
            insertUserToLocal(remote)
            remote
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserRole(userId: String): UserRole = withContext(Dispatchers.IO) {
        try {
            val user = api.fetchUser(userId)
            insertUserToLocal(user)
            user.role
        } catch (e: Exception) {
            UserRole.LEARNER
        }
    }

    override suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        api.updateUser(user)
        insertUserToLocal(user)
    }

    private fun insertUserToLocal(user: User) {
        database.appDatabaseQueries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role
        )
    }
}
