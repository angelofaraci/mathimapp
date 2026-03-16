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
        return api.createExercise(exercise)
    }

    override suspend fun updateExercise(exercise: Exercise): Exercise {
        return api.updateExercise(exercise)
    }

    override suspend fun deleteExercise(id: String) {
        api.deleteExercise(id)
    }
}
