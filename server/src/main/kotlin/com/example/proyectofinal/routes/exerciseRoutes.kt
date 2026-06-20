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

fun Application.exerciseRoutes() {
    routing {
        authenticate("auth-jwt") {
            get("/lessons/{lessonId}/exercises") {
                val lessonId = call.parameters["lessonId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val hideAnswers = call.currentRole() == UserRole.LEARNER

                val exercises = dbQuery {
                    Exercises.selectAll().where { Exercises.lessonId eq lessonId }
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
                }
                call.respond(exercises)
            }

            post("/exercises") {
                val request = call.receive<CreateExerciseRequest>()

                val creatorId = dbQuery {
                    (Lessons innerJoin Courses)
                        .select(Courses.creatorId)
                        .where { Lessons.id eq request.lessonId }
                        .firstOrNull()
                        ?.get(Courses.creatorId)
                } ?: return@post call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@post

                dbQuery {
                    Exercises.insert {
                        it[Exercises.id] = request.id
                        it[Exercises.lessonId] = request.lessonId
                        it[Exercises.question] = request.question
                        it[Exercises.options] = request.options.joinToString(",")
                        it[Exercises.correctAnswer] = request.correctAnswer
                        it[Exercises.type] = request.type.name
                    }
                }

                call.respond(
                    Exercise(
                        id = request.id,
                        lessonId = request.lessonId,
                        question = request.question,
                        options = request.options,
                        correctAnswer = request.correctAnswer,
                        type = request.type
                    )
                )
            }

            put("/exercises/{id}") {
                val exerciseId = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateExerciseRequest>()

                val creatorId = dbQuery {
                    (Exercises innerJoin Lessons innerJoin Courses)
                        .select(Courses.creatorId)
                        .where { Exercises.id eq exerciseId }
                        .firstOrNull()
                        ?.get(Courses.creatorId)
                } ?: return@put call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@put

                val updated = dbQuery {
                    Exercises.update({ Exercises.id eq exerciseId }) { row ->
                        request.question?.let { row[Exercises.question] = it }
                        request.options?.let { row[Exercises.options] = it.joinToString(",") }
                        request.correctAnswer?.let { row[Exercises.correctAnswer] = it }
                        request.type?.let { row[Exercises.type] = it.name }
                    }
                }

                if (updated == 0) {
                    call.respond(HttpStatusCode.NotFound)
                    return@put
                }

                val exercise = dbQuery {
                    val exerciseRow = Exercises.selectAll().where { Exercises.id eq exerciseId }.first()
                    Exercise(
                        id = exerciseRow[Exercises.id],
                        lessonId = exerciseRow[Exercises.lessonId],
                        question = exerciseRow[Exercises.question],
                        options = exerciseRow[Exercises.options].split(","),
                        correctAnswer = exerciseRow[Exercises.correctAnswer],
                        type = ExerciseType.valueOf(exerciseRow[Exercises.type])
                    )
                }
                call.respond(exercise)
            }

            delete("/exercises/{id}") {
                val exerciseId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val creatorId = dbQuery {
                    (Exercises innerJoin Lessons innerJoin Courses)
                        .select(Courses.creatorId)
                        .where { Exercises.id eq exerciseId }
                        .firstOrNull()
                        ?.get(Courses.creatorId)
                } ?: return@delete call.respond(HttpStatusCode.NotFound)

                if (!call.requireSelfOrAdmin(creatorId)) return@delete

                val deleted = dbQuery {
                    Exercises.deleteWhere { Exercises.id eq exerciseId }
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
