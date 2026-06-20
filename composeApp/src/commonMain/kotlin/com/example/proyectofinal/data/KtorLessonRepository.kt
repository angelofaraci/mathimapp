package com.example.proyectofinal.data

import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.models.Lesson

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
        return try {
            val lesson = api.fetchLesson(id)
            lesson.let {
                database.appDatabaseQueries.insertLesson(
                    id = it.id,
                    courseId = it.courseId,
                    title = it.title,
                    theoryContent = it.theoryContent
                )
            }
            lesson
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createLesson(lesson: Lesson): Lesson {
        val created = api.createLesson(lesson)
        database.appDatabaseQueries.insertLesson(
            id = created.id,
            courseId = created.courseId,
            title = created.title,
            theoryContent = created.theoryContent
        )
        return created
    }

    override suspend fun updateLesson(lesson: Lesson): Lesson {
        val updated = api.updateLesson(lesson)
        database.appDatabaseQueries.insertLesson(
            id = updated.id,
            courseId = updated.courseId,
            title = updated.title,
            theoryContent = updated.theoryContent
        )
        return updated
    }

    override suspend fun deleteLesson(id: String) {
        api.deleteLesson(id)
        database.appDatabaseQueries.deleteLesson(id)
    }
}
