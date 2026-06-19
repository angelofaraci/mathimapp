package com.example.proyectofinal.domain

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

    /**
     * Deletes an exercise.
     */
    suspend fun deleteExercise(id: String)
}
