package com.example.proyectofinal.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        allowHost("10.0.2.2:8080") // Android emulator localhost
        allowHost("localhost:5173") // Vite dev server
        
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("Access-Control-Allow-Origin")
        
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
}
