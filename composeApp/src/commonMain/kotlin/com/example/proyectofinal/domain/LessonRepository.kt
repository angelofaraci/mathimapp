package com.example.proyectofinal.domain

import com.example.proyectofinal.models.Lesson

interface LessonRepository {
    /**
     * Gets all lessons for a specific course.
     */
    suspend fun getLessonsByCourse(courseId: String): List<Lesson>

    /**
     * Gets a specific lesson by its ID.
     */
    suspend fun getLessonById(id: String): Lesson?

    /**
     * Adds a new lesson to a course.
     */
    suspend fun createLesson(lesson: Lesson): Lesson

    /**
     * Updates an existing lesson's theory or title.
     */
    suspend fun updateLesson(lesson: Lesson): Lesson

    /**
     * Updates lesson theory content without changing other lesson fields.
     */
    suspend fun updateTheory(lessonId: String, content: String): Lesson

    /**
     * Deletes a lesson.
     */
    suspend fun deleteLesson(id: String)
}
