package com.example.proyectofinal.domain

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
}
