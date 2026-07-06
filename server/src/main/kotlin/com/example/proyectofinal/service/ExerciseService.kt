package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.CreateAdminExerciseRequest
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.UpdateAdminExerciseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface AdminExerciseMutationResult {
    data class Success(val exercise: Exercise) : AdminExerciseMutationResult
    data class InvalidRequest(val message: String) : AdminExerciseMutationResult
    object NotFound : AdminExerciseMutationResult
}

class ExerciseService {
    fun getExercisesByLessonId(lessonId: String, hideAnswers: Boolean): List<Exercise> = dbQuery {
        Exercises.selectAll()
            .where { Exercises.lessonId eq lessonId }
            .map { it.toExercise(hideAnswers) }
    }

    fun listExercisesAdmin(lessonId: String? = null): List<Exercise> = dbQuery {
        val query = Exercises.selectAll()

        val filtered = if (lessonId == null) {
            query
        } else {
            query.where { Exercises.lessonId eq lessonId }
        }

        filtered.map { it.toExercise() }
    }

    fun createExercise(request: CreateExerciseRequest): Exercise = dbQuery {
        createExerciseInTransaction(request)
    }

    private fun createExerciseInTransaction(request: CreateExerciseRequest): Exercise {
        Exercises.insert {
            it[Exercises.id] = request.id
            it[Exercises.lessonId] = request.lessonId
            it[Exercises.question] = request.question
            it[Exercises.options] = request.options.joinToString(",")
            it[Exercises.correctAnswer] = request.correctAnswer
            it[Exercises.type] = request.type.name
        }

        return Exercise(
            id = request.id,
            lessonId = request.lessonId,
            question = request.question,
            options = request.options,
            correctAnswer = request.correctAnswer,
            type = request.type
        )
    }

    fun adminCreateExercise(request: CreateAdminExerciseRequest): AdminExerciseMutationResult = dbQuery {
        if (!lessonExists(request.lessonId)) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest("lessonId references an unknown lesson")
        }

        AdminExerciseMutationResult.Success(
            createExerciseInTransaction(
                CreateExerciseRequest(
                    id = request.id,
                    lessonId = request.lessonId,
                    question = request.question,
                    options = request.options,
                    correctAnswer = request.correctAnswer,
                    type = request.type
                )
            )
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

    fun adminUpdateExercise(id: String, request: UpdateAdminExerciseRequest): AdminExerciseMutationResult = dbQuery {
        if (request.lessonId != null && !lessonExists(request.lessonId)) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest("lessonId references an unknown lesson")
        }

        val updated = Exercises.update({ Exercises.id eq id }) { row ->
            request.lessonId?.let { row[Exercises.lessonId] = it }
            request.question?.let { row[Exercises.question] = it }
            request.options?.let { row[Exercises.options] = it.joinToString(",") }
            request.correctAnswer?.let { row[Exercises.correctAnswer] = it }
            request.type?.let { row[Exercises.type] = it.name }
        }

        if (updated == 0) {
            return@dbQuery AdminExerciseMutationResult.NotFound
        }

        val exercise = Exercises.selectAll()
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.toExercise()
            ?: return@dbQuery AdminExerciseMutationResult.NotFound

        AdminExerciseMutationResult.Success(exercise)
    }

    fun adminDeleteExercise(id: String): Boolean = deleteExercise(id)

    fun deleteExercise(id: String): Boolean = dbQuery {
        Exercises.deleteWhere { Exercises.id eq id } > 0
    }

    fun getLessonCreatorId(lessonId: String): String? = dbQuery {
        resolveLessonMutationOwnerId(lessonId)
    }

    fun getCreatorId(id: String): String? = dbQuery {
        val lessonId = Exercises.select(Exercises.lessonId)
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.get(Exercises.lessonId)
            ?: return@dbQuery null

        resolveLessonMutationOwnerId(lessonId)
    }

    private fun lessonExists(lessonId: String): Boolean =
        Lessons.selectAll()
            .where { Lessons.id eq lessonId }
            .count() > 0

    private fun resolveLessonMutationOwnerId(lessonId: String): String? {
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq lessonId }
            .firstOrNull()
            ?: return null

        val courseId = lesson[Lessons.courseId]

        return if (courseId != null) {
            Courses.select(Courses.creatorId)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?.get(Courses.creatorId)
                ?: lesson[Lessons.creatorId]
        } else {
            lesson[Lessons.creatorId]
        }
    }
}
