package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.LearningPath
import com.example.proyectofinal.models.LearningPathIdRequest
import com.example.proyectofinal.models.LearningPathState
import com.example.proyectofinal.models.LearningPathSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LearningPathApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {
    private val baseUrl = apiConfig.baseUrl

    suspend fun fetchPaths(gradeLevel: Int? = null): List<LearningPathSummary> =
        client.get("$baseUrl/learning-paths") {
            gradeLevel?.let { parameter("gradeLevel", it) }
        }.body()

    suspend fun fetchRecommendedPath(gradeLevel: Int): LearningPath =
        client.get("$baseUrl/learning-paths/recommended") {
            parameter("gradeLevel", gradeLevel)
        }.body()

    suspend fun fetchPath(pathId: String): LearningPath =
        client.get("$baseUrl/learning-paths/$pathId").body()

    suspend fun fetchState(gradeLevel: Int? = null): LearningPathState =
        client.get("$baseUrl/learning-paths/state") {
            gradeLevel?.let { parameter("gradeLevel", it) }
        }.body()

    suspend fun selectPath(pathId: String): LearningPathState =
        client.put("$baseUrl/learning-paths/selection") {
            contentType(ContentType.Application.Json)
            setBody(LearningPathIdRequest(pathId))
        }.body()

    suspend fun recordPathOpened(pathId: String): LearningPathState =
        client.post("$baseUrl/learning-paths/$pathId/opened").body()
}
