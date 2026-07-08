package com.example.proyectofinal.data

import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.models.Exercise

class KtorExerciseRepository(
    private val api: ExerciseApi,
    private val database: AppDatabase
) : ExerciseRepository {

    override suspend fun getExercisesByLesson(lessonId: String): List<Exercise> {
        val remote = api.fetchExercisesByLesson(lessonId)
        remote.forEach(::cacheExercise)
        return remote
    }

    override suspend fun createExercise(exercise: Exercise): Exercise {
        val created = api.createExercise(exercise)
        cacheExercise(created)
        return created
    }

    override suspend fun updateExercise(exercise: Exercise): Exercise {
        val updated = api.updateExercise(exercise)
        cacheExercise(updated)
        return updated
    }

    override suspend fun deleteExercise(id: String) {
        api.deleteExercise(id)
        database.appDatabaseQueries.deleteExercise(id)
    }

    private fun cacheExercise(exercise: Exercise) {
        database.appDatabaseQueries.insertExercise(
            id = exercise.id,
            lessonId = exercise.lessonId,
            title = exercise.title,
            type = exercise.type,
            payload = ExercisePayloadJson.encode(exercise)
        )
    }
}
