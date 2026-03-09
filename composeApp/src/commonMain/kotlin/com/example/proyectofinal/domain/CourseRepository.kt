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
}
