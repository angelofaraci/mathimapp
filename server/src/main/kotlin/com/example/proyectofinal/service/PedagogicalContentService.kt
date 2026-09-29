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
import com.example.proyectofinal.models.TheorySectionInput
import com.example.proyectofinal.models.TheorySectionType
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.util.UUID

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

sealed interface AdminTheorySectionsResult {
    data class Success(val response: LessonTheoryResponse) : AdminTheorySectionsResult
    data class InvalidRequest(val message: String) : AdminTheorySectionsResult
    data object NotFound : AdminTheorySectionsResult
}

class PedagogicalContentService {
    fun getTheoryAdmin(lessonId: String): AdminTheorySectionsResult = dbQuery {
        if (Lessons.select(Lessons.id).where { Lessons.id eq lessonId }.empty()) {
            return@dbQuery AdminTheorySectionsResult.NotFound
        }
        AdminTheorySectionsResult.Success(LessonTheoryResponse(lessonId, theorySections(lessonId)))
    }

    /** Replaces the complete ordered collection in one transaction. Legacy theoryContent is untouched. */
    fun replaceTheorySectionsAdmin(
        lessonId: String,
        inputs: List<TheorySectionInput>
    ): AdminTheorySectionsResult = dbQuery {
        if (Lessons.select(Lessons.id).where { Lessons.id eq lessonId }.empty()) {
            return@dbQuery AdminTheorySectionsResult.NotFound
        }
        validateTheorySections(inputs)?.let { return@dbQuery AdminTheorySectionsResult.InvalidRequest(it) }

        LessonTheorySections.deleteWhere { LessonTheorySections.lessonId eq lessonId }
        inputs.forEachIndexed { position, section ->
            LessonTheorySections.insert {
                it[id] = UUID.randomUUID().toString()
                it[LessonTheorySections.lessonId] = lessonId
                it[LessonTheorySections.position] = position
                it[type] = section.type.name
                it[title] = section.title?.trim()?.takeIf(String::isNotEmpty)
                it[content] = section.content.trim()
            }
        }
        AdminTheorySectionsResult.Success(LessonTheoryResponse(lessonId, theorySections(lessonId)))
    }

    fun getTheory(lessonId: String, userId: String, role: UserRole): TheoryReadResult = dbQuery {
        val access = lessonAccess(lessonId) ?: return@dbQuery TheoryReadResult.NotFound
        if (!canReadLessonContent(access, userId, role)) return@dbQuery TheoryReadResult.Forbidden

        val sections = theorySections(lessonId)

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

    private fun theorySections(lessonId: String): List<TheorySection> =
        LessonTheorySections.selectAll()
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

    private fun validateTheorySections(inputs: List<TheorySectionInput>): String? {
        if (inputs.size > 50) return "A lesson can have at most 50 theory sections"
        inputs.forEachIndexed { index, section ->
            if (section.content.isBlank()) return "Section ${index + 1} content cannot be blank"
            if (section.content.length > 20_000) return "Section ${index + 1} content is too long"
            if ((section.title?.length ?: 0) > 160) return "Section ${index + 1} title is too long"
        }
        return null
    }

    private data class HintRow(
        val id: String,
        val content: String,
        val position: Int,
        val unlockAfterAttempts: Int
    )
}
