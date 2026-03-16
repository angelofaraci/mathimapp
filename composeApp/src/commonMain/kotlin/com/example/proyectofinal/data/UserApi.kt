package com.example.proyectofinal.data

import com.example.proyectofinal.domain.User
import com.example.proyectofinal.domain.UserProgress
import com.example.proyectofinal.domain.UserRole
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(private val client: HttpClient) {
    private val baseUrl = "https://api.yourbackend.com"

    // --- User Profile & Roles ---
    
    suspend fun fetchUser(userId: String): User {
        return client.get("$baseUrl/users/$userId").body()
    }

    suspend fun updateUser(user: User) {
        client.put("$baseUrl/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    // --- User Progress ---

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$baseUrl/progress/$userId").body()
    }

    suspend fun saveUserProgress(progress: UserProgress) {
        client.post("$baseUrl/progress") {
            contentType(ContentType.Application.Json)
            setBody(progress)
        }
    }
}
