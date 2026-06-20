package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl

    suspend fun fetchUser(userId: String): User {
        return client.get("$baseUrl/users/$userId").body()
    }

    suspend fun updateUser(user: User) {
        client.put("$baseUrl/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$baseUrl/progress/$userId").body()
    }

    suspend fun saveUserProgress(request: CompleteLessonRequest) {
        client.post("$baseUrl/progress") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
