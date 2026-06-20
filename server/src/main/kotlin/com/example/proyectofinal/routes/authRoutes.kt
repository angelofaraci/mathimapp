package com.example.proyectofinal.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.models.*
import com.example.proyectofinal.plugins.Security
import com.example.proyectofinal.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.authRoutes(service: AuthService) {
    routing {
        post("/auth/register") {
            val request = call.receive<RegisterRequest>()

            if (request.role == UserRole.ADMIN) {
                call.respond(HttpStatusCode.Forbidden, "Public ADMIN registration is not allowed")
                return@post
            }

            val existingUser = service.findUserByEmail(request.email)
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, "Email already registered")
                return@post
            }

            val userId = UUID.randomUUID().toString()
            val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

            val user = service.createUser(userId = userId, request = request, passwordHash = passwordHash)

            val token = Security.generateToken(userId, request.role.name)

            call.respond(AuthResponse(token = token, user = user))
        }

        post("/auth/login") {
            val request = call.receive<LoginRequest>()

            val user = service.validateCredentials(request.email, request.password)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
                return@post
            }

            val token = Security.generateToken(user.id, user.role.name)

            call.respond(AuthResponse(token = token, user = user))
        }
    }
}
