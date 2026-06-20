package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.ExerciseService
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

fun Application.exerciseRoutes(service: ExerciseService) {
    routing {
        authenticate("auth-jwt") {
            get("/lessons/{lessonId}/exercises") {
                val lessonId = call.parameters["lessonId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val hideAnswers = call.currentRole() == UserRole.LEARNER

                call.respond(service.getExercisesByLessonId(lessonId, hideAnswers))
            }

            post("/exercises") {
                val request = call.receive<CreateExerciseRequest>()

                val creatorId = service.getLessonCreatorId(request.lessonId)
                    ?: return@post call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@post

                call.respond(service.createExercise(request))
            }

            put("/exercises/{id}") {
                val exerciseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateExerciseRequest>()

                val creatorId = service.getCreatorId(exerciseId) ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val exercise = service.updateExercise(exerciseId, request)
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
