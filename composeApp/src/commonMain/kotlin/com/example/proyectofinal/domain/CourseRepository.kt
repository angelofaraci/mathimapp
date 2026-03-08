package com.example.proyectofinal.domain

interface CourseRepository {
    /**
     * Fetches all available courses for the user to browse.
     */
    suspend fun getCourses(): List<Course>

    /**
     * Fetches a specific course by its ID.
     */
    suspend fun getCourseById(id: String): Course?

    /**
     * Fetches the user's current progress.
     */
    suspend fun getUserProgress(userId: String): UserProgress

    /**
     * Updates or saves the user's progress (e.g., when a lesson is finished).
     * This is the "Update/Create" part of your CRUD.
     */
    suspend fun saveProgress(progress: UserProgress)
}
