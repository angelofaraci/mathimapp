package com.example.proyectofinal

import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.plugins.configureCors
import com.example.proyectofinal.plugins.configureSecurity
import com.example.proyectofinal.routes.adminRoutes
import com.example.proyectofinal.routes.authRoutes
import com.example.proyectofinal.routes.courseRoutes
import com.example.proyectofinal.routes.exerciseRoutes
import com.example.proyectofinal.routes.lessonRoutes
import com.example.proyectofinal.routes.userRoutes
import com.example.proyectofinal.seed.SeedData
import com.example.proyectofinal.service.AuthService
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.ExerciseService
import com.example.proyectofinal.service.LessonService
import com.example.proyectofinal.service.UserService
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

    val authService = AuthService()
    val userService = UserService()
    val courseService = CourseService()
    val lessonService = LessonService()
    val exerciseService = ExerciseService()

    authRoutes(authService)
    userRoutes(userService)
    courseRoutes(courseService)
    lessonRoutes(lessonService)
    exerciseRoutes(exerciseService, lessonService)
    adminRoutes(userService, courseService)

    if (seedData) {
        SeedData.seedOfficialCourses()
    }
}
