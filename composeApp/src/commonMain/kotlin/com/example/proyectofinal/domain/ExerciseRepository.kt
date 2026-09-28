package com.example.proyectofinal.domain

import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.NextExerciseHintResponse

interface ExerciseRepository {
    /**
     * Gets all exercises for a lesson.
     */
    suspend fun getExercisesByLesson(lessonId: String): List<Exercise>

    /**
     * Creates a new exercise.
     */
    suspend fun createExercise(exercise: Exercise): Exercise

    /**
     * Updates an exercise.
     */
    suspend fun updateExercise(exercise: Exercise): Exercise

    /** Reveals the next server-authorized hint for an exercise. */
    suspend fun revealNextHint(exerciseId: String): NextExerciseHintResponse =
        NextExerciseHintResponse()

    /**
     * Deletes an exercise.
     */
    suspend fun deleteExercise(id: String)
}
