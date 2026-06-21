package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorUserRepository(
    private val api: UserApi,
    private val database: AppDatabase
) : UserRepository {

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        try {
            val remote = api.fetchUser("current-user-id")
            insertUserToLocal(remote)
            remote
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserRole(userId: String): UserRole = withContext(Dispatchers.IO) {
        try {
            val user = api.fetchUser(userId)
            insertUserToLocal(user)
            user.role
        } catch (e: Exception) {
            UserRole.LEARNER
        }
    }

    override suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        api.updateUser(user)
        insertUserToLocal(user)
    }

    override suspend fun getUserProgress(userId: String): UserProgress = withContext(Dispatchers.IO) {
        try {
            val remote = api.fetchUserProgress(userId)
            syncUserProgressToLocal(remote)
            remote
        } catch (e: Exception) {
            readUserProgressFromLocal(userId)
        }
    }

    override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse = withContext(Dispatchers.IO) {
        val response = api.completeExercise(exerciseId = exerciseId, score = score)
        syncUserProgressToLocal(response.progress)
        response
    }

    private fun insertUserToLocal(user: User) {
        database.appDatabaseQueries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role
        )
    }

    private fun syncUserProgressToLocal(progress: UserProgress) {
        database.appDatabaseQueries.insertProgress(
            userId = progress.userId,
            totalScore = progress.totalScore
        )
        progress.completedLessonIds.forEach { lessonId ->
            database.appDatabaseQueries.insertCompletedLesson(
                userId = progress.userId,
                lessonId = lessonId
            )
        }
        progress.completedExerciseIds.forEach { exerciseId ->
            database.appDatabaseQueries.insertCompletedExercise(
                userId = progress.userId,
                exerciseId = exerciseId
            )
        }
        progress.enrolledCourseIds.forEach { courseId ->
            database.appDatabaseQueries.insertEnrolledCourse(
                userId = progress.userId,
                courseId = courseId
            )
        }
    }

    private fun readUserProgressFromLocal(userId: String): UserProgress {
        val progress = database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOneOrNull()
        val completedLessonIds = database.appDatabaseQueries
            .selectCompletedLessonsByUserId(userId)
            .executeAsList()
            .toSet()
        val completedExerciseIds = database.appDatabaseQueries
            .selectCompletedExercisesByUserId(userId)
            .executeAsList()
            .toSet()
        val enrolledCourseIds = database.appDatabaseQueries
            .selectEnrolledCoursesByUserId(userId)
            .executeAsList()
            .toSet()

        return UserProgress(
            userId = userId,
            completedLessonIds = completedLessonIds,
            completedExerciseIds = completedExerciseIds,
            totalScore = progress?.totalScore ?: 0,
            enrolledCourseIds = enrolledCourseIds
        )
    }
}
