package com.example.proyectofinal.data

import com.example.proyectofinal.BASE_URL
import com.example.proyectofinal.domain.User
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.domain.UserProgress
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(private val client: HttpClient) {

    suspend fun fetchUser(userId: String): User {
        return client.get("$BASE_URL/users/$userId").body()
    }

    suspend fun updateUser(user: User) {
        client.put("$BASE_URL/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$BASE_URL/progress/$userId").body()
    }

    suspend fun saveUserProgress(request: CompleteLessonRequest) {
        client.post("$BASE_URL/progress") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
