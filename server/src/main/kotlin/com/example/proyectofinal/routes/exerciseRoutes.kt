package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.ExerciseService
import com.example.proyectofinal.service.LessonReadResult
import com.example.proyectofinal.service.LessonService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.exerciseRoutes(service: ExerciseService, lessonService: LessonService) {
    routing {
        authenticate("auth-jwt") {
            get("/lessons/{lessonId}/exercises") {
                val lessonId = call.parameters["lessonId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                when (val result = lessonService.getLessonByIdForUser(lessonId, userId, role)) {
                    is LessonReadResult.Success -> call.respond(result.lesson.exercises)
                    LessonReadResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    LessonReadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/exercises") {
                val request = try {
                    call.receive<CreateExerciseRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                val creatorId = service.getLessonCreatorId(request.lessonId)
                    ?: return@post call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@post

                val created = try {
                    service.createExercise(request)
                } catch (exception: IllegalArgumentException) {
                    return@post call.respond(HttpStatusCode.BadRequest, exception.message ?: "Invalid exercise payload")
                }

                call.respond(created)
            }

            put("/exercises/{id}") {
                val exerciseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = try {
                    call.receive<UpdateExerciseRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }

                val creatorId = service.getCreatorId(exerciseId) ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val exercise = try {
                    service.updateExercise(exerciseId, request)
                } catch (exception: IllegalArgumentException) {
                    return@put call.respond(HttpStatusCode.BadRequest, exception.message ?: "Invalid exercise payload")
                }
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(exercise)
            }

            delete("/exercises/{id}") {
                val exerciseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = service.getCreatorId(exerciseId) ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                if (!service.deleteExercise(exerciseId)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
