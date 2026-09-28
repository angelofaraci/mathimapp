package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.ExerciseHints
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.LessonTheorySections
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.UserExerciseAttempts
import com.example.proyectofinal.database.UserExerciseHintReveals
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.ExerciseHint
import com.example.proyectofinal.models.LessonTheoryResponse
import com.example.proyectofinal.models.NextExerciseHintResponse
import com.example.proyectofinal.models.TheorySection
import com.example.proyectofinal.models.TheorySectionType
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

sealed interface TheoryReadResult {
    data class Success(val response: LessonTheoryResponse) : TheoryReadResult
    data object Forbidden : TheoryReadResult
    data object NotFound : TheoryReadResult
}

sealed interface NextHintResult {
    data class Success(val response: NextExerciseHintResponse) : NextHintResult
    data object Forbidden : NextHintResult
    data object NotFound : NextHintResult
}

class PedagogicalContentService {
    fun getTheory(lessonId: String, userId: String, role: UserRole): TheoryReadResult = dbQuery {
        val access = lessonAccess(lessonId) ?: return@dbQuery TheoryReadResult.NotFound
        if (!canReadLessonContent(access, userId, role)) return@dbQuery TheoryReadResult.Forbidden

        val sections = LessonTheorySections.selectAll()
            .where { LessonTheorySections.lessonId eq lessonId }
            .orderBy(LessonTheorySections.position)
            .map { row ->
                TheorySection(
                    id = row[LessonTheorySections.id],
                    type = TheorySectionType.valueOf(row[LessonTheorySections.type]),
                    title = row[LessonTheorySections.title],
                    content = row[LessonTheorySections.content],
                    position = row[LessonTheorySections.position]
                )
            }

        TheoryReadResult.Success(LessonTheoryResponse(lessonId = lessonId, sections = sections))
    }

    fun revealNextHint(exerciseId: String, userId: String, role: UserRole): NextHintResult = dbQuery {
        val exercise = (Exercises innerJoin Lessons)
            .selectAll()
            .where { Exercises.id eq exerciseId }
            .firstOrNull()
            ?: return@dbQuery NextHintResult.NotFound
        val access = lessonAccess(exercise[Exercises.lessonId]) ?: return@dbQuery NextHintResult.NotFound
        if (!canReadLessonContent(access, userId, role)) return@dbQuery NextHintResult.Forbidden

        val validAttemptCount = UserExerciseAttempts.selectAll()
            .where {
                (UserExerciseAttempts.userId eq userId) and
                    (UserExerciseAttempts.exerciseId eq exerciseId)
            }
            .count()
            .toInt()
        val revealedHintIds = UserExerciseHintReveals.selectAll()
            .where {
                (UserExerciseHintReveals.userId eq userId) and
                    (UserExerciseHintReveals.exerciseId eq exerciseId)
            }
            .map { it[UserExerciseHintReveals.hintId] }
            .toSet()
        val availableHints = ExerciseHints.selectAll()
            .where { ExerciseHints.exerciseId eq exerciseId }
            .map { row ->
                HintRow(
                    id = row[ExerciseHints.id],
                    content = row[ExerciseHints.content],
                    position = row[ExerciseHints.position],
                    unlockAfterAttempts = row[ExerciseHints.unlockAfterAttempts]
                )
            }
            .filter { it.unlockAfterAttempts <= validAttemptCount && it.id !in revealedHintIds }
            .sortedBy(HintRow::position)

        val next = availableHints.firstOrNull()
            ?: return@dbQuery NextHintResult.Success(NextExerciseHintResponse())

        UserExerciseHintReveals.insert {
            it[UserExerciseHintReveals.userId] = userId
            it[UserExerciseHintReveals.exerciseId] = exerciseId
            it[UserExerciseHintReveals.hintId] = next.id
        }
        NextHintResult.Success(
            NextExerciseHintResponse(
                hint = ExerciseHint(id = next.id, content = next.content, position = next.position),
                remainingHints = availableHints.size - 1
            )
        )
    }

    private fun lessonAccess(lessonId: String): LessonContentAccess? {
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq lessonId }
            .firstOrNull()
            ?: return null
        val courseId = lesson[Lessons.courseId]
        if (courseId == null) {
            return lesson[Lessons.creatorId]?.let(LessonContentAccess::Standalone)
        }
        val course = Courses.select(Courses.creatorId, Courses.isOfficial)
            .where { Courses.id eq courseId }
            .firstOrNull()
            ?: return null
        return LessonContentAccess.CourseLinked(
            CourseContentAccess(
                courseId = courseId,
                creatorId = course[Courses.creatorId],
                isOfficial = course[Courses.isOfficial]
            )
        )
    }

    private data class HintRow(
        val id: String,
        val content: String,
        val position: Int,
        val unlockAfterAttempts: Int
    )
}
