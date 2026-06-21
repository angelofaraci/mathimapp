package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.TheoryUpdateRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.LessonListReadResult
import com.example.proyectofinal.service.LessonReadResult
import com.example.proyectofinal.service.LessonService
import com.example.proyectofinal.service.TheoryUpdateResult
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
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                when (val result = service.getLessonsByCourseIdForUser(courseId, userId, role)) {
                    is LessonListReadResult.Success -> call.respond(result.lessons)
                    LessonListReadResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    LessonListReadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                when (val result = service.getLessonByIdForUser(lessonId, userId, role)) {
                    is LessonReadResult.Success -> call.respond(result.lesson)
                    LessonReadResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    LessonReadResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
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

            put("/lessons/{id}/theory") {
                val lessonId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<TheoryUpdateRequest>()

                if (lessonId != request.lessonId) {
                    return@put call.respond(HttpStatusCode.BadRequest, "Path id must match body lessonId")
                }

                val userId = call.currentUserId()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val role = call.currentRole()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")

                if (role != UserRole.ADMIN && role != UserRole.TEACHER) {
                    return@put call.respond(HttpStatusCode.Forbidden, "Forbidden")
                }

                when (val result = service.updateTheoryContent(lessonId, request.theoryContent, userId, role)) {
                    is TheoryUpdateResult.Success -> call.respond(result.lesson)
                    TheoryUpdateResult.Forbidden -> call.respond(HttpStatusCode.Forbidden, "Forbidden")
                    TheoryUpdateResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                }
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
