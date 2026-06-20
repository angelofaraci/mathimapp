package com.example.proyectofinal.routes

import com.example.proyectofinal.database.*
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.UserProgress as DTOUserProgress
import com.example.proyectofinal.plugins.currentRole
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.eq


fun Application.userRoutes() {
    routing {
        authenticate("auth-jwt") {
            get("/users/{id}") {
                val userId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                val user = dbQuery {
                    val userRow = Users.selectAll().where { Users.id eq userId }.firstOrNull()
                        ?: return@dbQuery null

                    User(
                        id = userRow[Users.id],
                        name = userRow[Users.name],
                        email = userRow[Users.email],
                        role = UserRole.valueOf(userRow[Users.role])
                    )
                } ?: return@get call.respond(HttpStatusCode.NotFound)

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

                val updated = dbQuery {
                    Users.update({ Users.id eq userId }) { row ->
                        request.name?.let { row[Users.name] = it }
                        request.email?.let { row[Users.email] = it }
                        request.role?.let { row[Users.role] = it.name }
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val user = dbQuery {
                    val userRow = Users.selectAll().where { Users.id eq userId }.first()
                    User(
                        id = userRow[Users.id],
                        name = userRow[Users.name],
                        email = userRow[Users.email],
                        role = UserRole.valueOf(userRow[Users.role])
                    )
                }
                call.respond(user)
            }

            get("/progress/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                val progress = dbQuery {
                    val progressRow = UserProgress.selectAll().where { UserProgress.userId eq userId }.firstOrNull()
                    val completedLessons = CompletedLessons.selectAll().where { CompletedLessons.userId eq userId }
                        .map { it[CompletedLessons.lessonId] }.toSet()
                    val enrolledCourses = EnrolledCourses.selectAll().where { EnrolledCourses.userId eq userId }
                        .map { it[EnrolledCourses.courseId] }.toSet()

                    val totalScore = progressRow?.let { it[UserProgress.totalScore] } ?: 0

                    DTOUserProgress(
                        userId = userId,
                        totalScore = totalScore,
                        completedLessonIds = completedLessons,
                        enrolledCourseIds = enrolledCourses
                    )
                }

                call.respond(progress)
            }

            post("/progress") {
                val request = call.receive<CompleteLessonRequest>()
                if (!call.requireSelfOrAdmin(request.userId)) return@post

                dbQuery {
                    val existingProgress = UserProgress.selectAll().where { UserProgress.userId eq request.userId }.firstOrNull()
                    if (existingProgress == null) {
                        UserProgress.insert {
                            it[UserProgress.userId] = request.userId
                            it[UserProgress.totalScore] = request.score
                        }
                    } else {
                        val currentScore = existingProgress[UserProgress.totalScore]
                        UserProgress.update({ UserProgress.userId eq request.userId }) { row ->
                            row[UserProgress.totalScore] = currentScore + request.score
                        }
                    }

                    CompletedLessons.insert {
                        it[CompletedLessons.userId] = request.userId
                        it[CompletedLessons.lessonId] = request.lessonId
                    }
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
