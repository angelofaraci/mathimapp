package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.LoginRequest
import com.example.proyectofinal.models.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class AuthApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl

    suspend fun login(email: String, password: String): AuthResponse =
        postAuth(
            path = "/auth/login",
            body = LoginRequest(email = email, password = password)
        )

    suspend fun register(name: String, email: String, password: String): AuthResponse =
        postAuth(
            path = "/auth/register",
            body = RegisterRequest(name = name, email = email, password = password)
        )

    private suspend fun postAuth(path: String, body: Any): AuthResponse {
        val response = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        return response.toAuthResponse()
    }

    private suspend fun HttpResponse.toAuthResponse(): AuthResponse {
        if (!status.isSuccess()) {
            val rawError = bodyAsText().ifBlank {
                "Request failed with status ${status.value}"
            }
            throw AuthApiException(rawError)
        }

        return body()
    }
}

class AuthApiException(message: String) : Exception(message)
