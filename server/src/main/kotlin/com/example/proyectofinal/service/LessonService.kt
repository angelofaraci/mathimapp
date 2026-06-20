package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface TheoryUpdateResult {
    data class Success(val lesson: Lesson) : TheoryUpdateResult
    object Forbidden : TheoryUpdateResult
    object NotFound : TheoryUpdateResult
}

class LessonService {
    fun getLessonsByCourseId(courseId: String): List<Lesson> = dbQuery {
        Lessons.selectAll()
            .where { Lessons.courseId eq courseId }
            .orderBy(Lessons.orderIndex)
            .map { it.toLesson() }
    }

    fun getLessonById(id: String, hideAnswers: Boolean): Lesson? = dbQuery {
        val lesson = Lessons.selectAll()
            .where { Lessons.id eq id }
            .firstOrNull()
            ?: return@dbQuery null

        val exercises = Exercises.selectAll()
            .where { Exercises.lessonId eq id }
            .map { it.toExercise(hideAnswers) }

        lesson.toLesson(exercises)
    }

    fun createLesson(request: CreateLessonRequest): Lesson = dbQuery {
        val maxOrder = Lessons.selectAll()
            .where { Lessons.courseId eq request.courseId }
            .orderBy(Lessons.orderIndex, SortOrder.DESC)
            .firstOrNull()
            ?.let { it[Lessons.orderIndex] + 1 }
            ?: 0

        Lessons.insert {
            it[Lessons.id] = request.id
            it[Lessons.courseId] = request.courseId
            it[Lessons.title] = request.title
            it[Lessons.theoryContent] = request.theoryContent
            it[Lessons.orderIndex] = maxOrder
        }

        Lesson(
            id = request.id,
            courseId = request.courseId,
            title = request.title,
            theoryContent = request.theoryContent
        )
    }

    fun updateLesson(id: String, request: UpdateLessonRequest): Lesson? {
        val updated = dbQuery {
            Lessons.update({ Lessons.id eq id }) { row ->
                request.title?.let { row[Lessons.title] = it }
                request.theoryContent?.let { row[Lessons.theoryContent] = it }
            }
        }

        if (updated == 0) {
            return null
        }

        return dbQuery {
            Lessons.selectAll()
                .where { Lessons.id eq id }
                .first()
                .toLesson()
        }
    }

    fun deleteLesson(id: String): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id eq id } > 0
    }

    fun updateTheoryContent(
        lessonId: String,
        content: String,
        userId: String,
        role: UserRole
    ): TheoryUpdateResult {
        val lessonAccess = dbQuery {
            (Lessons innerJoin Courses)
                .select(Lessons.id, Courses.creatorId, Courses.isOfficial)
                .where { Lessons.id eq lessonId }
                .firstOrNull()
                ?.let {
                    LessonTheoryAccess(
                        creatorId = it[Courses.creatorId],
                        isOfficial = it[Courses.isOfficial]
                    )
                }
        } ?: return TheoryUpdateResult.NotFound

        val canUpdateTheory = when (role) {
            UserRole.ADMIN -> lessonAccess.isOfficial
            UserRole.TEACHER -> lessonAccess.creatorId == userId
            UserRole.LEARNER -> false
        }

        if (!canUpdateTheory) {
            return TheoryUpdateResult.Forbidden
        }

        val updated = dbQuery {
            Lessons.update({ Lessons.id eq lessonId }) { row ->
                row[theoryContent] = content
            }
        }

        if (updated == 0) {
            return TheoryUpdateResult.NotFound
        }

        return getLessonById(lessonId, hideAnswers = false)
            ?.let { TheoryUpdateResult.Success(it) }
            ?: TheoryUpdateResult.NotFound
    }

    fun getCourseCreatorId(courseId: String): String? = dbQuery {
        Courses.selectAll()
            .where { Courses.id eq courseId }
            .firstOrNull()
            ?.get(Courses.creatorId)
    }

    fun getCreatorId(id: String): String? = dbQuery {
        (Lessons innerJoin Courses)
            .select(Courses.creatorId)
            .where { Lessons.id eq id }
            .firstOrNull()
            ?.get(Courses.creatorId)
    }

    private data class LessonTheoryAccess(
        val creatorId: String,
        val isOfficial: Boolean
    )
}
