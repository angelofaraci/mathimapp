package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CompleteExerciseRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.UserService
import com.example.proyectofinal.service.ExerciseCompletionResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val CurrentUserAlias = "current-user-id"

fun Application.userRoutes(service: UserService) {
    routing {
        authenticate("auth-jwt") {
            get("/users/{id}") {
                val requestedUserId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = if (requestedUserId == CurrentUserAlias) {
                    call.currentUserId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                } else {
                    requestedUserId
                }

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

            post("/exercises/{id}/complete") {
                val exerciseId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<CompleteExerciseRequest>()
                if (exerciseId != request.exerciseId) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Path id must match body exerciseId")
                }
                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                if (role != UserRole.STUDENT) {
                    return@post call.respond(HttpStatusCode.Forbidden, "Only students can complete exercises")
                }
                when (val result = service.completeExercise(userId, role, request)) {
                    is ExerciseCompletionResult.Success -> call.respond(result.response)
                    ExerciseCompletionResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    ExerciseCompletionResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }
            post("/progress") {
                val currentRole = call.currentRole() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                if (currentRole == UserRole.STUDENT) {
                    return@post call.respond(
                        HttpStatusCode.Gone,
                        "Direct lesson completion is deprecated for students; complete exercises instead"
                    )
                }
                if (currentRole != UserRole.ADMIN) {
                    return@post call.respond(HttpStatusCode.Forbidden, "Forbidden")
                }
                val request = call.receive<CompleteLessonRequest>()
                service.updateProgress(request)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
