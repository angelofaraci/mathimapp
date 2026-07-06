package com.example.proyectofinal.service

import com.example.proyectofinal.database.CompletedExercises
import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.UserProgress as UserProgressTable
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.AdminUserResponse
import com.example.proyectofinal.models.CompleteExerciseRequest
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.PageResponse
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface ExerciseCompletionResult {
    data class Success(val response: ExerciseCompletionResponse) : ExerciseCompletionResult
    object Forbidden : ExerciseCompletionResult
    object NotFound : ExerciseCompletionResult
}

class UserService {
    fun getUserById(id: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .firstOrNull()
            ?.toUser()
    }

    fun listUsers(query: String? = null, page: Int = 0, size: Int = 20): PageResponse<AdminUserResponse> = dbQuery {
        val searchPattern = query?.let { "%$it%" }

        val totalElements = if (searchPattern != null) {
            Users.selectAll()
                .where { (Users.name like searchPattern) or (Users.email like searchPattern) }
                .count()
        } else {
            Users.selectAll().count()
        }

        val items = (if (searchPattern != null) {
            Users.selectAll()
                .where { (Users.name like searchPattern) or (Users.email like searchPattern) }
        } else {
            Users.selectAll()
        }).orderBy(Users.name)
            .limit(size)
            .offset((page * size).toLong())
            .map { row ->
                AdminUserResponse(
                    id = row[Users.id],
                    name = row[Users.name],
                    email = row[Users.email],
                    role = UserRole.parse(row[Users.role]) ?: UserRole.STUDENT
                )
            }

        PageResponse(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
        )
    }

    fun updateUser(id: String, request: UpdateUserRequest): User? {
        val updated = dbQuery {
            Users.update({ Users.id eq id }) { row ->
                request.name?.let { row[Users.name] = it }
                request.email?.let { row[Users.email] = it }
                request.role?.let { row[Users.role] = it.name }
            }
        }

        if (updated == 0) {
            return null
        }

        return getUserById(id)
    }

    fun getUserProgress(userId: String): UserProgress = dbQuery {
        readUserProgress(userId)
    }

    fun completeExercise(
        userId: String,
        role: UserRole,
        request: CompleteExerciseRequest
    ): ExerciseCompletionResult = dbQuery {
        if (role != UserRole.STUDENT) {
            return@dbQuery ExerciseCompletionResult.Forbidden
        }
        val exerciseRow = (Exercises innerJoin Lessons)
            .select(Exercises.id, Exercises.lessonId, Lessons.courseId, Lessons.creatorId)
            .where { Exercises.id eq request.exerciseId }
            .firstOrNull()
            ?: return@dbQuery ExerciseCompletionResult.NotFound
        val lessonAccess = resolveLessonContentAccess(
            courseId = exerciseRow[Lessons.courseId],
            standaloneCreatorId = exerciseRow[Lessons.creatorId]
        ) ?: return@dbQuery ExerciseCompletionResult.NotFound
        val exerciseAccess = ExerciseCompletionAccess(
            exerciseId = exerciseRow[Exercises.id],
            lessonId = exerciseRow[Exercises.lessonId],
            lessonAccess = lessonAccess
        )
        if (!canReadLessonContent(exerciseAccess.lessonAccess, userId, role)) {
            return@dbQuery ExerciseCompletionResult.Forbidden
        }
        val existingCompletion = CompletedExercises.selectAll()
            .where {
                (CompletedExercises.userId eq userId) and
                    (CompletedExercises.exerciseId eq request.exerciseId)
            }
            .firstOrNull()
        if (existingCompletion == null) {
            CompletedExercises.insert {
                it[CompletedExercises.userId] = userId
                it[CompletedExercises.exerciseId] = request.exerciseId
                it[CompletedExercises.score] = request.score
            }
            val existingProgress = UserProgressTable.selectAll()
                .where { UserProgressTable.userId eq userId }
                .firstOrNull()
            if (existingProgress == null) {
                UserProgressTable.insert {
                    it[UserProgressTable.userId] = userId
                    it[UserProgressTable.totalScore] = request.score
                }
            } else {
                val currentScore = existingProgress[UserProgressTable.totalScore]
                UserProgressTable.update({ UserProgressTable.userId eq userId }) { row ->
                    row[UserProgressTable.totalScore] = currentScore + request.score
                }
            }
        }
        val totalExercisesInLesson = Exercises.selectAll()
            .where { Exercises.lessonId eq exerciseAccess.lessonId }
            .count()
        val completedExercisesInLesson = (CompletedExercises innerJoin Exercises)
            .selectAll()
            .where {
                (CompletedExercises.userId eq userId) and
                    (Exercises.lessonId eq exerciseAccess.lessonId)
            }
            .count()
        val lessonCompleted = totalExercisesInLesson > 0 && completedExercisesInLesson == totalExercisesInLesson
        if (lessonCompleted) {
            val existingLessonCompletion = CompletedLessons.selectAll()
                .where {
                    (CompletedLessons.userId eq userId) and
                        (CompletedLessons.lessonId eq exerciseAccess.lessonId)
                }
                .firstOrNull()
            if (existingLessonCompletion == null) {
                CompletedLessons.insert {
                    it[CompletedLessons.userId] = userId
                    it[CompletedLessons.lessonId] = exerciseAccess.lessonId
                }
            }
        }
        ExerciseCompletionResult.Success(
            ExerciseCompletionResponse(
                exerciseId = exerciseAccess.exerciseId,
                lessonId = exerciseAccess.lessonId,
                lessonCompleted = lessonCompleted,
                progress = readUserProgress(userId)
            )
        )
    }

    private fun readUserProgress(userId: String): UserProgress {
        val progressRow = UserProgressTable.selectAll()
            .where { UserProgressTable.userId eq userId }
            .firstOrNull()
        val completedLessons = CompletedLessons.selectAll()
            .where { CompletedLessons.userId eq userId }
            .map { it[CompletedLessons.lessonId] }
            .toSet()
        val completedExercises = CompletedExercises.selectAll()
            .where { CompletedExercises.userId eq userId }
            .map { it[CompletedExercises.exerciseId] }
            .toSet()
        val enrolledCourses = EnrolledCourses.selectAll()
            .where { EnrolledCourses.userId eq userId }
            .map { it[EnrolledCourses.courseId] }
            .toSet()
        return UserProgress(
            userId = userId,
            totalScore = progressRow?.get(UserProgressTable.totalScore) ?: 0,
            completedLessonIds = completedLessons,
            completedExerciseIds = completedExercises,
            enrolledCourseIds = enrolledCourses
        )
    }

    fun updateProgress(request: CompleteLessonRequest) {
        dbQuery {
            val existingProgress = UserProgressTable.selectAll()
                .where { UserProgressTable.userId eq request.userId }
                .firstOrNull()

            if (existingProgress == null) {
                UserProgressTable.insert {
                    it[UserProgressTable.userId] = request.userId
                    it[UserProgressTable.totalScore] = request.score
                }
            } else {
                val currentScore = existingProgress[UserProgressTable.totalScore]
                UserProgressTable.update({ UserProgressTable.userId eq request.userId }) { row ->
                    row[UserProgressTable.totalScore] = currentScore + request.score
                }
            }
            CompletedLessons.insert {
                it[CompletedLessons.userId] = request.userId
                it[CompletedLessons.lessonId] = request.lessonId
            }
        }
    }

    private data class ExerciseCompletionAccess(
        val exerciseId: String,
        val lessonId: String,
        val lessonAccess: LessonContentAccess
    )

    private fun resolveLessonContentAccess(
        courseId: String?,
        standaloneCreatorId: String?
    ): LessonContentAccess? =
        if (courseId != null) {
            Courses.select(Courses.creatorId, Courses.isOfficial)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?.let {
                    LessonContentAccess.CourseLinked(
                        CourseContentAccess(
                            courseId = courseId,
                            creatorId = it[Courses.creatorId],
                            isOfficial = it[Courses.isOfficial]
                        )
                    )
                }
        } else {
            standaloneCreatorId?.let { LessonContentAccess.Standalone(it) }
        }
}
