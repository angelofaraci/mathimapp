package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.CreateAdminLessonRequest
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
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

sealed interface LessonReadResult {
    data class Success(val lesson: Lesson) : LessonReadResult
    object Forbidden : LessonReadResult
    object NotFound : LessonReadResult
}

sealed interface LessonListReadResult {
    data class Success(val lessons: List<Lesson>) : LessonListReadResult
    object Forbidden : LessonListReadResult
    object NotFound : LessonListReadResult
}

sealed interface FieldPatch<out T> {
    data object Unchanged : FieldPatch<Nothing>
    data class Present<T>(val value: T) : FieldPatch<T>
}

data class AdminLessonPatchRequest(
    val title: FieldPatch<String> = FieldPatch.Unchanged,
    val theoryContent: FieldPatch<String> = FieldPatch.Unchanged,
    val courseId: FieldPatch<String?> = FieldPatch.Unchanged,
    val creatorId: FieldPatch<String?> = FieldPatch.Unchanged
)

sealed interface AdminLessonMutationResult {
    data class Success(val lesson: Lesson) : AdminLessonMutationResult
    data class InvalidRequest(val message: String) : AdminLessonMutationResult
    object NotFound : AdminLessonMutationResult
}

class LessonService {
    fun getLessonsByCourseId(courseId: String): List<Lesson> = dbQuery {
        Lessons.selectAll()
            .where { Lessons.courseId eq courseId }
            .orderBy(Lessons.orderIndex)
            .map { it.toLesson() }
    }

    fun getLessonsByCourseIdAdmin(courseId: String): List<Lesson> = getLessonsByCourseId(courseId)

    fun listStandaloneLessons(): List<Lesson> = dbQuery {
        Lessons.selectAll()
            .where { Lessons.courseId.isNull() }
            .orderBy(Lessons.orderIndex)
            .map { it.toLesson() }
    }

    fun listLessonsAdmin(): List<Lesson> = dbQuery {
        Lessons.selectAll()
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

    fun getLessonsByCourseIdForUser(courseId: String, userId: String, role: UserRole): LessonListReadResult {
        val lessonAccess = dbQuery {
            Courses.select(Courses.id, Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?.let {
                    CourseContentAccess(
                        courseId = it[Courses.id],
                        creatorId = it[Courses.creatorId],
                        isOfficial = it[Courses.isOfficial]
                    )
                }
        } ?: return LessonListReadResult.NotFound

        if (!canReadCourseContent(lessonAccess, userId, role)) {
            return LessonListReadResult.Forbidden
        }

        return LessonListReadResult.Success(getLessonsByCourseId(courseId))
    }

    fun getLessonByIdForUser(id: String, userId: String, role: UserRole): LessonReadResult {
        val lessonAccess = resolveLessonReadAccess(id) ?: return LessonReadResult.NotFound

        if (!canReadLessonContent(lessonAccess, userId, role)) {
            return LessonReadResult.Forbidden
        }

        return getLessonById(id, hideAnswers = shouldHideLessonAnswers(lessonAccess, role))
            ?.let { LessonReadResult.Success(it) }
            ?: LessonReadResult.NotFound
    }

    fun createLesson(request: CreateLessonRequest): Lesson = dbQuery {
        createLessonInTransaction(request)
    }

    private fun createLessonInTransaction(request: CreateLessonRequest): Lesson {
        val maxOrder = request.courseId
            ?.let { courseId ->
                Lessons.selectAll()
                    .where { Lessons.courseId eq courseId }
                    .orderBy(Lessons.orderIndex, SortOrder.DESC)
                    .firstOrNull()
                    ?.let { it[Lessons.orderIndex] + 1 }
            }
            ?: 0

        Lessons.insert {
            it[Lessons.id] = request.id
            it[Lessons.courseId] = request.courseId
            it[Lessons.creatorId] = request.creatorId
            it[Lessons.title] = request.title
            it[Lessons.theoryContent] = request.theoryContent
            it[Lessons.orderIndex] = maxOrder
        }

        return Lesson(
            id = request.id,
            courseId = request.courseId,
            creatorId = request.creatorId,
            title = request.title,
            theoryContent = request.theoryContent
        )
    }

    fun adminCreateLesson(
        request: CreateAdminLessonRequest,
        authenticatedUserId: String
    ): AdminLessonMutationResult {
        val normalizedCourseId = try {
            normalizeNullableId(request.courseId, "courseId")
        } catch (exception: IllegalArgumentException) {
            return AdminLessonMutationResult.InvalidRequest(exception.message ?: "Invalid courseId")
        }

        val normalizedCreatorId = try {
            normalizeNullableId(request.creatorId, "creatorId")
        } catch (exception: IllegalArgumentException) {
            return AdminLessonMutationResult.InvalidRequest(exception.message ?: "Invalid creatorId")
        }

        if (request.title.isBlank()) {
            return AdminLessonMutationResult.InvalidRequest("title is required")
        }

        return dbQuery {
            if (normalizedCourseId != null && !courseExists(normalizedCourseId)) {
                return@dbQuery AdminLessonMutationResult.InvalidRequest("courseId references an unknown course")
            }

            val creatorId = normalizedCreatorId ?: authenticatedUserId

            AdminLessonMutationResult.Success(
                createLessonInTransaction(
                    CreateLessonRequest(
                        id = request.id,
                        courseId = normalizedCourseId,
                        title = request.title,
                        theoryContent = request.theoryContent,
                        creatorId = creatorId
                    )
                )
            )
        }
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

    fun adminUpdateLesson(id: String, request: AdminLessonPatchRequest): AdminLessonMutationResult = dbQuery {
        val lesson = Lessons.selectAll()
            .where { Lessons.id eq id }
            .firstOrNull()
            ?: return@dbQuery AdminLessonMutationResult.NotFound

        val existingCourseId = lesson[Lessons.courseId]
        val persistedCreatorId = lesson[Lessons.creatorId] ?: existingCourseId?.let(::resolveCourseCreatorId)

        val targetCourseId = when (val coursePatch = request.courseId) {
            FieldPatch.Unchanged -> existingCourseId
            is FieldPatch.Present -> try {
                normalizeNullableId(coursePatch.value, "courseId")
            } catch (exception: IllegalArgumentException) {
                return@dbQuery AdminLessonMutationResult.InvalidRequest(exception.message ?: "Invalid courseId")
            }
        }

        if (targetCourseId != null && !courseExists(targetCourseId)) {
            return@dbQuery AdminLessonMutationResult.InvalidRequest("courseId references an unknown course")
        }

        val targetCreatorId = when (val creatorPatch = request.creatorId) {
            FieldPatch.Unchanged -> persistedCreatorId
            is FieldPatch.Present -> {
                if (creatorPatch.value == null) {
                    return@dbQuery AdminLessonMutationResult.InvalidRequest(
                        "creatorId cannot be cleared; omit it to leave ownership unchanged"
                    )
                }

                try {
                    normalizeNullableId(creatorPatch.value, "creatorId")
                } catch (exception: IllegalArgumentException) {
                    return@dbQuery AdminLessonMutationResult.InvalidRequest(exception.message ?: "Invalid creatorId")
                }
            }
        }

        if (targetCreatorId == null) {
            return@dbQuery AdminLessonMutationResult.InvalidRequest(
                "creatorId is required when a lesson becomes standalone"
            )
        }

        val normalizedTitle = when (val titlePatch = request.title) {
            FieldPatch.Unchanged -> null
            is FieldPatch.Present -> titlePatch.value.also {
                if (it.isBlank()) {
                    return@dbQuery AdminLessonMutationResult.InvalidRequest("title cannot be blank")
                }
            }
        }

        val normalizedTheoryContent = when (val theoryPatch = request.theoryContent) {
            FieldPatch.Unchanged -> null
            is FieldPatch.Present -> theoryPatch.value
        }

        Lessons.update({ Lessons.id eq id }) { row ->
            normalizedTitle?.let { row[Lessons.title] = it }
            normalizedTheoryContent?.let { row[Lessons.theoryContent] = it }

            if (request.courseId !is FieldPatch.Unchanged) {
                row[Lessons.courseId] = targetCourseId
            }

            if (request.creatorId !is FieldPatch.Unchanged || lesson[Lessons.creatorId] != targetCreatorId) {
                row[Lessons.creatorId] = targetCreatorId
            }
        }

        val updatedLesson = Lessons.selectAll()
            .where { Lessons.id eq id }
            .firstOrNull()
            ?.toLesson()
            ?: return@dbQuery AdminLessonMutationResult.NotFound

        AdminLessonMutationResult.Success(updatedLesson)
    }

    fun deleteLessonAdmin(id: String): Boolean = deleteLesson(id)

    fun deleteLesson(id: String): Boolean = dbQuery {
        Lessons.deleteWhere { Lessons.id eq id } > 0
    }

    fun updateTheoryContent(
        lessonId: String,
        content: String,
        userId: String,
        role: UserRole
    ): TheoryUpdateResult {
        val lessonAccess = resolveTheoryAccess(lessonId) ?: return TheoryUpdateResult.NotFound

        val canUpdateTheory = when (lessonAccess) {
            is LessonTheoryAccess.CourseLinked -> when (role) {
                UserRole.ADMIN -> lessonAccess.isOfficial
                UserRole.TEACHER -> lessonAccess.creatorId == userId
                UserRole.STUDENT -> false
            }
            is LessonTheoryAccess.Standalone -> when (role) {
                UserRole.ADMIN -> true
                UserRole.TEACHER -> lessonAccess.creatorId == userId
                UserRole.STUDENT -> false
            }
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
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq id }
            .firstOrNull()
            ?: return@dbQuery null

        val courseId = lesson[Lessons.courseId]

        if (courseId != null) {
            resolveCourseCreatorId(courseId) ?: lesson[Lessons.creatorId]
        } else {
            lesson[Lessons.creatorId]
        }
    }

    private fun resolveLessonReadAccess(id: String): LessonContentAccess? = dbQuery {
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq id }
            .firstOrNull()
            ?: return@dbQuery null

        val courseId = lesson[Lessons.courseId]

        if (courseId != null) {
            val course = Courses.select(Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?: return@dbQuery null

            LessonContentAccess.CourseLinked(
                CourseContentAccess(
                    courseId = courseId,
                    creatorId = course[Courses.creatorId],
                    isOfficial = course[Courses.isOfficial]
                )
            )
        } else {
            val creatorId = lesson[Lessons.creatorId] ?: return@dbQuery null
            LessonContentAccess.Standalone(creatorId)
        }
    }

    private fun resolveTheoryAccess(lessonId: String): LessonTheoryAccess? = dbQuery {
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq lessonId }
            .firstOrNull()
            ?: return@dbQuery null

        val courseId = lesson[Lessons.courseId]

        if (courseId != null) {
            val course = Courses.select(Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?: return@dbQuery null

            LessonTheoryAccess.CourseLinked(
                creatorId = course[Courses.creatorId],
                isOfficial = course[Courses.isOfficial]
            )
        } else {
            val creatorId = lesson[Lessons.creatorId] ?: return@dbQuery null
            LessonTheoryAccess.Standalone(creatorId)
        }
    }

    private fun courseExists(courseId: String): Boolean =
        Courses.selectAll()
            .where { Courses.id eq courseId }
            .count() > 0

    private fun resolveCourseCreatorId(courseId: String): String? =
        Courses.select(Courses.creatorId)
            .where { Courses.id eq courseId }
            .firstOrNull()
            ?.get(Courses.creatorId)

    private fun normalizeNullableId(value: String?, fieldName: String): String? {
        if (value == null) {
            return null
        }

        return value.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("$fieldName cannot be blank")
    }

    private sealed interface LessonTheoryAccess {
        data class CourseLinked(val creatorId: String, val isOfficial: Boolean) : LessonTheoryAccess
        data class Standalone(val creatorId: String) : LessonTheoryAccess
    }
}
