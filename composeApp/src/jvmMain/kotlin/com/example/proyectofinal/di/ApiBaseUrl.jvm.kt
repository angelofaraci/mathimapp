package com.example.proyectofinal.di

private const val ApiBaseUrlProperty = "api.base.url"
private const val ApiBaseUrlEnvironment = "API_BASE_URL"
private const val DefaultJvmApiBaseUrl = "http://localhost:8080"

internal actual fun getApiBaseUrl(): String =
    System.getProperty(ApiBaseUrlProperty)
        ?: System.getenv(ApiBaseUrlEnvironment)
        ?: DefaultJvmApiBaseUrl
