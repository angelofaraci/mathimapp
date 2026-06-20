package com.example.proyectofinal

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val BASE_URL = "http://10.0.2.2:8080"

object TokenHolder {
    var accessToken: String? = null
}

private fun HttpClientConfig<*>.configureNetworkClient() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
        TokenHolder.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

fun createHttpClient(engine: HttpClientEngine? = null): HttpClient =
    if (engine != null) {
        HttpClient(engine) { configureNetworkClient() }
    } else {
        HttpClient { configureNetworkClient() }
    }

val httpClient = createHttpClient()
