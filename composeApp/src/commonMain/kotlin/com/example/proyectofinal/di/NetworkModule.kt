package com.example.proyectofinal.di

import com.example.proyectofinal.createHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule = module {
    single { ApiConfig(baseUrl = getApiBaseUrl()) }
    single<TokenStore> { InMemoryTokenStore() }
    single<HttpClient> { createHttpClient(tokenStore = get()) }
}
