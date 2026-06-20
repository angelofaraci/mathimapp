package com.example.proyectofinal.routes

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.*
import com.example.proyectofinal.plugins.requireSelfOrAdmin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList

fun Application.courseRoutes() {
    routing {
        authenticate("auth-jwt") {
            get("/courses/official") {
                val courses = dbQuery {
                    Courses.selectAll().where { Courses.isOfficial eq true }
                        .map { row ->
                            Course(
                                id = row[Courses.id],
                                title = row[Courses.title],
                                description = row[Courses.description],
                                creatorId = row[Courses.creatorId],
                                isOfficial = row[Courses.isOfficial],
                                joinCode = row[Courses.joinCode]
                            )
                        }
                }
                call.respond(courses)
            }

            get("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val course = dbQuery {
                    val courseRow = Courses.selectAll().where { Courses.id eq courseId }.firstOrNull()
                        ?: return@dbQuery null

                    val lessons = Lessons.selectAll().where { Lessons.courseId eq courseId }
                        .orderBy(Lessons.orderIndex)
                        .map { row ->
                            Lesson(
                                id = row[Lessons.id],
                                courseId = row[Lessons.courseId],
                                title = row[Lessons.title],
                                theoryContent = row[Lessons.theoryContent]
                            )
                        }

                    Course(
                        id = courseRow[Courses.id],
                        title = courseRow[Courses.title],
                        description = courseRow[Courses.description],
                        creatorId = courseRow[Courses.creatorId],
                        isOfficial = courseRow[Courses.isOfficial],
                        joinCode = courseRow[Courses.joinCode],
                        lessons = lessons
                    )
                } ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(course)
            }

            get("/courses/creator/{creatorId}") {
                val creatorId = call.parameters["creatorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(creatorId)) return@get

                val courses = dbQuery {
                    Courses.selectAll().where { Courses.creatorId eq creatorId }
                        .map { row ->
                            Course(
                                id = row[Courses.id],
                                title = row[Courses.title],
                                description = row[Courses.description],
                                creatorId = row[Courses.creatorId],
                                isOfficial = row[Courses.isOfficial],
                                joinCode = row[Courses.joinCode]
                            )
                        }
                }
                call.respond(courses)
            }

            get("/courses/enrolled/{userId}") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (!call.requireSelfOrAdmin(userId)) return@get

                val courses = dbQuery {
                    val enrolledCourseIds = EnrolledCourses.selectAll().where { EnrolledCourses.userId eq userId }
                        .map { it[EnrolledCourses.courseId] }

                    Courses.selectAll().where { Courses.id inList enrolledCourseIds }
                        .map { row ->
                            Course(
                                id = row[Courses.id],
                                title = row[Courses.title],
                                description = row[Courses.description],
                                creatorId = row[Courses.creatorId],
                                isOfficial = row[Courses.isOfficial],
                                joinCode = row[Courses.joinCode]
                            )
                        }
                }
                call.respond(courses)
            }

            post("/courses") {
                val request = call.receive<CreateCourseRequest>()
                if (!call.requireSelfOrAdmin(request.creatorId)) return@post

                dbQuery {
                    Courses.insert {
                        it[Courses.id] = request.id
                        it[Courses.title] = request.title
                        it[Courses.description] = request.description
                        it[Courses.creatorId] = request.creatorId
                        it[Courses.isOfficial] = request.isOfficial
                        it[Courses.joinCode] = request.joinCode
                    }
                }

                call.respond(
                    Course(
                        id = request.id,
                        title = request.title,
                        description = request.description,
                        creatorId = request.creatorId,
                        isOfficial = request.isOfficial,
                        joinCode = request.joinCode
                    )
                )
            }

            put("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateCourseRequest>()

                val creatorId = dbQuery {
                    Courses.selectAll().where { Courses.id eq courseId }.firstOrNull()?.get(Courses.creatorId)
                } ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val updated = dbQuery {
                    Courses.update({ Courses.id eq courseId }) { row ->
                        request.title?.let { row[Courses.title] = it }
                        request.description?.let { row[Courses.description] = it }
                        request.joinCode?.let { row[Courses.joinCode] = it }
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val course = dbQuery {
                    val courseRow = Courses.selectAll().where { Courses.id eq courseId }.first()
                    Course(
                        id = courseRow[Courses.id],
                        title = courseRow[Courses.title],
                        description = courseRow[Courses.description],
                        creatorId = courseRow[Courses.creatorId],
                        isOfficial = courseRow[Courses.isOfficial],
                        joinCode = courseRow[Courses.joinCode]
                    )
                }
                call.respond(course)
            }

            delete("/courses/{id}") {
                val courseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = dbQuery {
                    Courses.selectAll().where { Courses.id eq courseId }.firstOrNull()?.get(Courses.creatorId)
                } ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                val deleted = dbQuery {
                    Courses.deleteWhere { Courses.id eq courseId }
                }
                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }

            post("/courses/join") {
                val request = call.receive<JoinCourseRequest>()
                if (!call.requireSelfOrAdmin(request.userId)) return@post

                val course = dbQuery {
                    val course = Courses.selectAll().where { Courses.joinCode eq request.code }.firstOrNull()
                        ?: return@dbQuery null

                    EnrolledCourses.insert {
                        it[EnrolledCourses.userId] = request.userId
                        it[EnrolledCourses.courseId] = course[Courses.id]
                    }

                    Course(
                        id = course[Courses.id],
                        title = course[Courses.title],
                        description = course[Courses.description],
                        creatorId = course[Courses.creatorId],
                        isOfficial = course[Courses.isOfficial],
                        joinCode = course[Courses.joinCode]
                    )
                } ?: return@post call.respond(HttpStatusCode.NotFound, "Invalid join code")

                call.respond(course)
            }
        }
    }
}
