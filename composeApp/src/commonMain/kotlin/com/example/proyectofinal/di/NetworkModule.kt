package com.example.proyectofinal.di

import com.example.proyectofinal.createHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

private const val DefaultBaseUrl = "http://10.0.2.2:8080"

val networkModule = module {
    single { ApiConfig(baseUrl = DefaultBaseUrl) }
    single<TokenStore> { InMemoryTokenStore() }
    single<HttpClient> { createHttpClient(tokenStore = get()) }
}
