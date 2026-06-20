package com.example.proyectofinal.routes

import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.userRoutes(service: UserService) {
    routing {
        authenticate("auth-jwt") {
            get("/users/{id}") {
                val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                val user = service.getUserById(userId) ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(user)
            }

            put("/users/{id}") {
                val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@put

                val request = call.receive<UpdateUserRequest>()
                val currentRole = call.currentRole() ?: return@put call.respond(HttpStatusCode.Unauthorized)

                if (request.role != null && currentRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, "Only admins can change roles")
                    return@put
                }

                val user = service.updateUser(userId, request) ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(user)
            }

            get("/progress/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                call.respond(service.getUserProgress(userId))
            }

            post("/progress") {
                val request = call.receive<CompleteLessonRequest>()
                if (!call.requireSelfOrAdmin(request.userId)) return@post

                service.updateProgress(request)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
