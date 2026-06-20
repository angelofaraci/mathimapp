package com.example.proyectofinal.routes

import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.JoinCourseRequest
import com.example.proyectofinal.models.UpdateCourseRequest
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import com.example.proyectofinal.service.CourseService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.routing

fun Application.courseRoutes(service: CourseService) {
    routing {
        authenticate("auth-jwt") {
            get("/courses/official") {
                call.respond(service.getOfficialCourses())
            }

            get("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val course = service.getCourseById(courseId) ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(course)
            }

            get("/courses/creator/{creatorId}") {
                val creatorId = call.parameters["creatorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(creatorId)) return@get

                call.respond(service.getCoursesByCreator(creatorId))
            }

            get("/courses/enrolled/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                call.respond(service.getEnrolledCourses(userId))
            }

            post("/courses") {
                val request = call.receive<CreateCourseRequest>()
                if (!call.requireSelfOrAdmin(request.creatorId)) return@post

                call.respond(service.createCourse(request))
            }

            put("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateCourseRequest>()

                val creatorId = service.getCreatorId(courseId) ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val course = service.updateCourse(courseId, request) ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(course)
            }

            delete("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = service.getCreatorId(courseId) ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                if (!service.deleteCourse(courseId)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }

            post("/courses/join") {
                val request = call.receive<JoinCourseRequest>()
                if (!call.requireSelfOrAdmin(request.userId)) return@post

                val course = service.joinCourse(request.userId, request.code)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Invalid join code")

                call.respond(course)
            }
        }
    }
}
