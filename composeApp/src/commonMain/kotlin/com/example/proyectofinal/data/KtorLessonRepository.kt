package com.example.proyectofinal.data

import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.TheoryUpdateRequest

class KtorLessonRepository(
    private val api: LessonApi,
    private val database: AppDatabase
) : LessonRepository {

    override suspend fun getLessonsByCourse(courseId: String): List<Lesson> {
        val lessons = api.fetchLessonsByCourse(courseId)
        lessons.forEach(::cacheLesson)
        return lessons
    }

    override suspend fun getLessonById(id: String): Lesson? {
        return try {
            val lesson = api.fetchLesson(id)
            cacheLesson(lesson)
            lesson
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createLesson(lesson: Lesson): Lesson {
        val created = api.createLesson(lesson)
        cacheLesson(created)
        return created
    }

    override suspend fun updateLesson(lesson: Lesson): Lesson {
        val updated = api.updateLesson(lesson)
        cacheLesson(updated)
        return updated
    }

    override suspend fun updateTheory(lessonId: String, content: String): Lesson {
        val updated = api.updateTheory(
            TheoryUpdateRequest(
                lessonId = lessonId,
                theoryContent = content
            )
        )
        cacheLesson(updated)
        return updated
    }

    override suspend fun deleteLesson(id: String) {
        api.deleteLesson(id)
        database.appDatabaseQueries.deleteLesson(id)
    }

    private fun cacheLesson(lesson: Lesson) {
        database.appDatabaseQueries.insertLesson(
            id = lesson.id,
            courseId = lesson.courseId,
            creatorId = lesson.creatorId,
            title = lesson.title,
            theoryContent = lesson.theoryContent
        )
    }
}
