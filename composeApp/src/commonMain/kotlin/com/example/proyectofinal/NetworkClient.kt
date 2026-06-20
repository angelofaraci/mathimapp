package com.example.proyectofinal

import com.example.proyectofinal.di.TokenStore
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private fun HttpClientConfig<*>.configureNetworkClient(tokenStore: TokenStore) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
        tokenStore.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
            headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

fun createHttpClient(
    tokenStore: TokenStore,
    engine: HttpClientEngine? = null
): HttpClient =
    if (engine != null) {
        HttpClient(engine) { configureNetworkClient(tokenStore) }
    } else {
        HttpClient { configureNetworkClient(tokenStore) }
    }
