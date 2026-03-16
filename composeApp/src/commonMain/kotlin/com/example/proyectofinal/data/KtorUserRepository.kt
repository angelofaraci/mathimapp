package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.domain.User
import com.example.proyectofinal.domain.UserRole
import com.example.proyectofinal.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorUserRepository(
    private val api: UserApi,
    private val database: AppDatabase
) : UserRepository {

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        // In a real app, you would get the current user ID from a SessionManager/Prefs
        // For now, we simulate fetching a default user
        try {
            val remote = api.fetchUser("current-user-id")
            insertUserToLocal(remote)
            remote
        } catch (e: Exception) {
            // Fallback to local database if offline
            database.appDatabaseQueries.selectAllCourses().executeAsOneOrNull() // Placeholder for selectUser
            null
        }
    }

    override suspend fun getUserRole(userId: String): UserRole = withContext(Dispatchers.IO) {
        try {
            val user = api.fetchUser(userId)
            insertUserToLocal(user)
            user.role
        } catch (e: Exception) {
            UserRole.LEARNER // Default fallback
        }
    }

    override suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        api.updateUser(user)
        insertUserToLocal(user)
    }

    private fun insertUserToLocal(user: User) {
        database.appDatabaseQueries.insertProgress(
            userId = user.id,
            totalScore = 0 // Scores would be handled by ProgressRepository
        )
        // Note: You might want to add a specific insertUser query to your .sq 
        // if you want to cache the full User object (name, email, role).
    }
}
