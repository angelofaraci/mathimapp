package com.example.proyectofinal.routes

import com.example.proyectofinal.models.RoleUpdateRequest
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.requireAdmin
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.adminRoutes(userService: UserService, courseService: CourseService) {
    routing {
        authenticate("auth-jwt") {
            get("/admin/users") {
                if (!call.requireAdmin()) return@get

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
                val query = call.request.queryParameters["query"]

                val safeSize = size.coerceIn(1, 100)

                call.respond(userService.listUsers(query = query, page = page, size = safeSize))
            }

            get("/admin/courses") {
                if (!call.requireAdmin()) return@get

                call.respond(courseService.getAllCoursesAdmin())
            }

            put("/admin/users/{id}/role") {
                if (!call.requireAdmin()) return@put

                val userId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<RoleUpdateRequest>()

                val role = UserRole.parse(request.role)
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid role")

                val updated = userService.updateUser(userId, UpdateUserRequest(role = role))
                    ?: return@put call.respond(HttpStatusCode.NotFound)

                call.respond(updated)
            }
        }
    }
}
