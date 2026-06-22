package com.example.proyectofinal.di

private const val DefaultIosApiBaseUrl = "http://localhost:8080"

internal actual fun getApiBaseUrl(): String = DefaultIosApiBaseUrl
