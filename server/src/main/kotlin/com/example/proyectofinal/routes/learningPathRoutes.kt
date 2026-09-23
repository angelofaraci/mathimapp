package com.example.proyectofinal.routes

import com.example.proyectofinal.models.LearningPathIdRequest
import com.example.proyectofinal.plugins.currentUserId
import com.example.proyectofinal.service.LearningPathService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

/** Authenticated learner access to platform-curated learning paths only. */
fun Application.learningPathRoutes(service: LearningPathService) {
    routing {
        authenticate("auth-jwt") {
            get("/learning-paths") {
                val gradeLevel = call.optionalGradeLevel() ?: return@get
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                call.respond(service.listPathsForUser(userId, gradeLevel))
            }

            get("/learning-paths/recommended") {
                val gradeLevel = call.requiredGradeLevel() ?: return@get
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val path = service.resolveDefaultPathForUser(gradeLevel, userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(path)
            }

            get("/learning-paths/state") {
                val gradeLevel = call.optionalGradeLevel() ?: return@get
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                call.respond(service.getState(userId, gradeLevel))
            }

            get("/learning-paths/{id}") {
                val pathId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val path = service.getPathForUser(pathId, userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(path)
            }

            put("/learning-paths/selection") {
                val userId = call.currentUserId()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                val request = try {
                    call.receive<LearningPathIdRequest>()
                } catch (_: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                }
                if (request.pathId.isBlank() || !service.selectPath(userId, request.pathId)) {
                    return@put call.respond(HttpStatusCode.NotFound)
                }
                call.respond(service.getState(userId))
            }

            post("/learning-paths/{id}/opened") {
                val pathId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.currentUserId()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
                if (!service.recordPathOpened(userId, pathId)) {
                    return@post call.respond(HttpStatusCode.NotFound)
                }
                call.respond(service.getState(userId))
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.optionalGradeLevel(): Int? {
    val raw = request.queryParameters["gradeLevel"] ?: return null
    return raw.toIntOrNull()?.takeIf { it > 0 }
        ?: run {
            respond(HttpStatusCode.BadRequest, "gradeLevel must be a positive integer")
            null
        }
}

private suspend fun io.ktor.server.application.ApplicationCall.requiredGradeLevel(): Int? {
    if (request.queryParameters["gradeLevel"] == null) {
        respond(HttpStatusCode.BadRequest, "gradeLevel is required")
        return null
    }
    return optionalGradeLevel()
}
