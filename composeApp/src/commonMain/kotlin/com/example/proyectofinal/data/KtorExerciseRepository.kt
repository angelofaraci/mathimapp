package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Exercise
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.db.AppDatabase

class KtorExerciseRepository(
    private val api: ExerciseApi,
    private val database: AppDatabase
) : ExerciseRepository {

    override suspend fun getExercisesByLesson(lessonId: String): List<Exercise> {
        val remote = api.fetchExercisesByLesson(lessonId)
        remote.forEach { exercise ->
            database.appDatabaseQueries.insertExercise(
                id = exercise.id,
                lessonId = exercise.lessonId,
                question = exercise.question,
                correctAnswer = exercise.correctAnswer,
                type = exercise.type,
                options = exercise.options.joinToString(",")
            )
        }
        return remote
    }

    override suspend fun createExercise(exercise: Exercise): Exercise {
        val created = api.createExercise(exercise)
        database.appDatabaseQueries.insertExercise(
            id = created.id,
            lessonId = created.lessonId,
            question = created.question,
            correctAnswer = created.correctAnswer,
            type = created.type,
            options = created.options.joinToString(",")
        )
        return created
    }

    override suspend fun updateExercise(exercise: Exercise): Exercise {
        val updated = api.updateExercise(exercise)
        database.appDatabaseQueries.insertExercise(
            id = updated.id,
            lessonId = updated.lessonId,
            question = updated.question,
            correctAnswer = updated.correctAnswer,
            type = updated.type,
            options = updated.options.joinToString(",")
        )
        return updated
    }

    override suspend fun deleteExercise(id: String) {
        api.deleteExercise(id)
        database.appDatabaseQueries.deleteExercise(id)
    }
}
