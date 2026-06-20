package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.requireSelfOrAdmin
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

fun Application.lessonRoutes(service: LessonService) {
    routing {
        authenticate("auth-jwt") {
            get("/courses/{courseId}/lessons") {
                val courseId = call.parameters["courseId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                call.respond(service.getLessonsByCourseId(courseId))
            }

            get("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val hideAnswers = call.currentRole() == UserRole.LEARNER

                val lesson = service.getLessonById(lessonId, hideAnswers)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(lesson)
            }

            post("/lessons") {
                val request = call.receive<CreateLessonRequest>()

                val creatorId = service.getCourseCreatorId(request.courseId)
                    ?: return@post call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@post

                call.respond(service.createLesson(request))
            }

            put("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateLessonRequest>()

                val creatorId = service.getCreatorId(lessonId) ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val lesson = service.updateLesson(lessonId, request) ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(lesson)
            }

            delete("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = service.getCreatorId(lessonId) ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                if (!service.deleteLesson(lessonId)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
