package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.UpdateExerciseRequest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExerciseService {
    fun getExercisesByLessonId(lessonId: String, hideAnswers: Boolean): List<Exercise> = dbQuery {
        Exercises.selectAll()
            .where { Exercises.lessonId eq lessonId }
            .map { it.toExercise(hideAnswers) }
    }

    fun createExercise(request: CreateExerciseRequest): Exercise = dbQuery {
        Exercises.insert {
            it[Exercises.id] = request.id
            it[Exercises.lessonId] = request.lessonId
            it[Exercises.question] = request.question
            it[Exercises.options] = request.options.joinToString(",")
            it[Exercises.correctAnswer] = request.correctAnswer
            it[Exercises.type] = request.type.name
        }

        Exercise(
            id = request.id,
            lessonId = request.lessonId,
            question = request.question,
            options = request.options,
            correctAnswer = request.correctAnswer,
            type = request.type
        )
    }

    fun updateExercise(id: String, request: UpdateExerciseRequest): Exercise? {
        val updated = dbQuery {
            Exercises.update({ Exercises.id eq id }) { row ->
                request.question?.let { row[Exercises.question] = it }
                request.options?.let { row[Exercises.options] = it.joinToString(",") }
                request.correctAnswer?.let { row[Exercises.correctAnswer] = it }
                request.type?.let { row[Exercises.type] = it.name }
            }
        }

        if (updated == 0) {
            return null
        }

        return dbQuery {
            Exercises.selectAll()
                .where { Exercises.id eq id }
                .first()
                .toExercise()
        }
    }

    fun deleteExercise(id: String): Boolean = dbQuery {
        Exercises.deleteWhere { Exercises.id eq id } > 0
    }

    fun getLessonCreatorId(lessonId: String): String? = dbQuery {
        (Lessons innerJoin Courses)
            .select(Courses.creatorId)
            .where { Lessons.id eq lessonId }
            .firstOrNull()
            ?.get(Courses.creatorId)
    }

    fun getCreatorId(id: String): String? = dbQuery {
        (Exercises innerJoin Lessons innerJoin Courses)
            .select(Courses.creatorId)
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.get(Courses.creatorId)
    }
}
