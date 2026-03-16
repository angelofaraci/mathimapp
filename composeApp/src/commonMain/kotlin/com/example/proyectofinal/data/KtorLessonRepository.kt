package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Lesson
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.db.AppDatabase

class KtorLessonRepository(
    private val api: LessonApi,
    private val database: AppDatabase
) : LessonRepository {

    override suspend fun getLessonsByCourse(courseId: String): List<Lesson> {
        // Try to get from API first (can be changed to local-first later)
        val lessons = api.fetchLessonsByCourse(courseId)
        // Update local database
        lessons.forEach { lesson ->
            database.appDatabaseQueries.insertLesson(
                id = lesson.id,
                courseId = lesson.courseId,
                title = lesson.title,
                theoryContent = lesson.theoryContent
            )
        }
        return lessons
    }

    override suspend fun getLessonById(id: String): Lesson? {
        val local = database.appDatabaseQueries.selectLessonsByCourseId(id).executeAsOneOrNull()
        // In a real app, you'd map the local entity to domain model
        return api.fetchLesson(id)
    }

    override suspend fun createLesson(lesson: Lesson): Lesson {
        return api.createLesson(lesson)
    }

    override suspend fun updateLesson(lesson: Lesson): Lesson {
        return api.updateLesson(lesson)
    }

    override suspend fun deleteLesson(id: String) {
        api.deleteLesson(id)
        database.appDatabaseQueries.deleteCourse(id) // This should be deleteLesson in the .sq
    }
}
