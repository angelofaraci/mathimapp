package com.example.proyectofinal.routes

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.*
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
import org.jetbrains.exposed.v1.core.SortOrder

fun Application.lessonRoutes() {
    routing {
        authenticate("auth-jwt") {
            get("/courses/{courseId}/lessons") {
                val courseId = call.parameters["courseId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                val lessons = dbQuery {
                    Lessons.selectAll().where { Lessons.courseId eq courseId }
                        .orderBy(Lessons.orderIndex)
                        .map { row ->
                            Lesson(
                                id = row[Lessons.id],
                                courseId = row[Lessons.courseId],
                                title = row[Lessons.title],
                                theoryContent = row[Lessons.theoryContent]
                            )
                        }
                }
                call.respond(lessons)
            }

            get("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val hideAnswers = call.currentRole() == UserRole.LEARNER

                val lesson = dbQuery {
                    val lessonRow = Lessons.selectAll().where { Lessons.id eq lessonId }.firstOrNull()
                        ?: return@dbQuery null

                    val exercises = Exercises.selectAll().where { Exercises.lessonId eq lessonId }
                        .map { row ->
                            Exercise(
                                id = row[Exercises.id],
                                lessonId = row[Exercises.lessonId],
                                question = row[Exercises.question],
                                options = row[Exercises.options].split(","),
                                correctAnswer = if (hideAnswers) "" else row[Exercises.correctAnswer],
                                type = ExerciseType.valueOf(row[Exercises.type])
                            )
                        }

                    Lesson(
                        id = lessonRow[Lessons.id],
                        courseId = lessonRow[Lessons.courseId],
                        title = lessonRow[Lessons.title],
                        theoryContent = lessonRow[Lessons.theoryContent],
                        exercises = exercises
                    )
                } ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(lesson)
            }

            post("/lessons") {
                val request = call.receive<CreateLessonRequest>()

                val creatorId = dbQuery {
                    Courses.selectAll().where { Courses.id eq request.courseId }.firstOrNull()?.get(Courses.creatorId)
                } ?: return@post call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@post

                dbQuery {
                    val maxOrder = Lessons.selectAll().where { Lessons.courseId eq request.courseId }
                        .orderBy(Lessons.orderIndex, SortOrder.DESC)
                        .firstOrNull()
                        ?.let { it[Lessons.orderIndex] + 1 } ?: 0

                    Lessons.insert {
                        it[Lessons.id] = request.id
                        it[Lessons.courseId] = request.courseId
                        it[Lessons.title] = request.title
                        it[Lessons.theoryContent] = request.theoryContent
                        it[Lessons.orderIndex] = maxOrder
                    }
                }

                call.respond(
                    Lesson(
                        id = request.id,
                        courseId = request.courseId,
                        title = request.title,
                        theoryContent = request.theoryContent
                    )
                )
            }

            put("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateLessonRequest>()

                val creatorId = dbQuery {
                    (Lessons innerJoin Courses)
                        .select(Courses.creatorId)
                        .where { Lessons.id eq lessonId }
                        .firstOrNull()
                        ?.get(Courses.creatorId)
                } ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val updated = dbQuery {
                    Lessons.update({ Lessons.id eq lessonId }) { row ->
                        request.title?.let { row[Lessons.title] = it }
                        request.theoryContent?.let { row[Lessons.theoryContent] = it }
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val lesson = dbQuery {
                    val lessonRow = Lessons.selectAll().where { Lessons.id eq lessonId }.first()
                    Lesson(
                        id = lessonRow[Lessons.id],
                        courseId = lessonRow[Lessons.courseId],
                        title = lessonRow[Lessons.title],
                        theoryContent = lessonRow[Lessons.theoryContent]
                    )
                }
                call.respond(lesson)
            }

            delete("/lessons/{id}") {
                val lessonId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = dbQuery {
                    (Lessons innerJoin Courses)
                        .select(Courses.creatorId)
                        .where { Lessons.id eq lessonId }
                        .firstOrNull()
                        ?.get(Courses.creatorId)
                } ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                val deleted = dbQuery {
                    Lessons.deleteWhere { Lessons.id eq lessonId }
                }
                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
