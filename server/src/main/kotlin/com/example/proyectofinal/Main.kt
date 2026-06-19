package com.example.proyectofinal

import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.plugins.configureCors
import com.example.proyectofinal.plugins.configureSecurity
import com.example.proyectofinal.routes.authRoutes
import com.example.proyectofinal.routes.courseRoutes
import com.example.proyectofinal.routes.exerciseRoutes
import com.example.proyectofinal.routes.lessonRoutes
import com.example.proyectofinal.routes.userRoutes
import com.example.proyectofinal.seed.SeedData
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module(
    initDatabase: Boolean = true,
    seedData: Boolean = true
) {
    if (initDatabase) {
        DatabaseFactory.init()
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    configureCors()
    configureSecurity()

    authRoutes()
    userRoutes()
    courseRoutes()
    lessonRoutes()
    exerciseRoutes()

    if (seedData) {
        SeedData.seedOfficialCourses()
    }
}
