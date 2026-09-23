package com.example.proyectofinal.data

import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.domain.LearningPathRepository
import com.example.proyectofinal.models.LearningPath
import com.example.proyectofinal.models.LearningPathLesson
import com.example.proyectofinal.models.LearningPathState
import com.example.proyectofinal.models.LearningPathSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorLearningPathRepository(
    private val api: LearningPathApi,
    private val database: AppDatabase
) : LearningPathRepository {
    override suspend fun getPaths(gradeLevel: Int?): List<LearningPathSummary> = withContext(Dispatchers.IO) {
        api.fetchPaths(gradeLevel).also { paths -> cacheCatalog(paths, gradeLevel) }
    }

    override suspend fun getRecommendedPath(gradeLevel: Int): LearningPath? = withContext(Dispatchers.IO) {
        try {
            api.fetchRecommendedPath(gradeLevel).also(::cachePath)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getPath(pathId: String): LearningPath? = withContext(Dispatchers.IO) {
        try {
            api.fetchPath(pathId).also(::cachePath)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getState(userId: String, gradeLevel: Int?): LearningPathState = withContext(Dispatchers.IO) {
        api.fetchState(gradeLevel).also { cacheState(userId, it) }
    }

    override suspend fun selectPath(userId: String, pathId: String): LearningPathState? = withContext(Dispatchers.IO) {
        try {
            api.selectPath(pathId).also { cacheState(userId, it) }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun recordPathOpened(userId: String, pathId: String): LearningPathState? = withContext(Dispatchers.IO) {
        try {
            api.recordPathOpened(pathId).also { cacheState(userId, it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheCatalog(paths: List<LearningPathSummary>, gradeLevel: Int?) {
        database.transaction {
            if (gradeLevel == null) {
                database.appDatabaseQueries.deleteAllLearningPaths()
            } else {
                database.appDatabaseQueries.deleteLearningPathsByGradeLevel(gradeLevel.toLong())
            }
            paths.forEach(::cacheSummary)
        }
    }

    private fun cachePath(path: LearningPath) {
        val queries = database.appDatabaseQueries
        val cachedSummary = queries.selectLearningPathById(path.id).executeAsOneOrNull()
        queries.insertLearningPath(
            id = path.id,
            name = path.name,
            description = path.description,
            visibleObjective = path.visibleObjective,
            objectiveType = path.objective.type.name,
            objectiveGradeLevel = path.objective.gradeLevel.toLong(),
            lessonCount = cachedSummary?.lessonCount ?: path.lessons.size.toLong(),
            completedLessonCount = cachedSummary?.completedLessonCount ?: path.lessons.count { it.completed }.toLong(),
            progressPercentage = cachedSummary?.progressPercentage ?: progress(path.lessons).toLong(),
            isDefaultForObjective = cachedSummary?.isDefaultForObjective ?: false
        )
        queries.deleteLearningPathLessons(path.id)
        path.lessons.forEach { lesson ->
            queries.insertLearningPathLesson(
                pathId = path.id,
                lessonId = lesson.lessonId,
                title = lesson.title,
                orderIndex = lesson.orderIndex.toLong(),
                completed = lesson.completed
            )
        }
    }

    private fun cacheSummary(path: LearningPathSummary) {
        database.appDatabaseQueries.insertLearningPath(
            id = path.id,
            name = path.name,
            description = path.description,
            visibleObjective = path.visibleObjective,
            objectiveType = path.objective.type.name,
            objectiveGradeLevel = path.objective.gradeLevel.toLong(),
            lessonCount = path.lessonCount.toLong(),
            completedLessonCount = path.completedLessonCount.toLong(),
            progressPercentage = path.progressPercentage.toLong(),
            isDefaultForObjective = path.isDefaultForObjective
        )
    }

    private fun cacheState(userId: String, state: LearningPathState) {
        require(userId.isNotBlank()) { "userId must not be blank" }
        database.appDatabaseQueries.insertLearningPathState(
            userId = userId,
            recommendedPathId = state.recommendedPathId,
            selectedPathId = state.selectedPathId,
            lastOpenedPathId = state.lastOpenedPathId
        )
    }

    private fun progress(lessons: List<LearningPathLesson>): Int =
        if (lessons.isEmpty()) 0 else lessons.count { it.completed } * 100 / lessons.size
}
