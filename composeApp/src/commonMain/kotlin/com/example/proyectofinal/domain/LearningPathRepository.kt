package com.example.proyectofinal.domain

import com.example.proyectofinal.models.LearningPath
import com.example.proyectofinal.models.LearningPathState
import com.example.proyectofinal.models.LearningPathSummary

/**
 * Accesses the platform-curated learning path catalog and learner-owned path state.
 *
 * The backend is authoritative for selected and last-opened paths. Local persistence is
 * intentionally a read cache, scoped by account for the state projection.
 */
interface LearningPathRepository {
    suspend fun getPaths(gradeLevel: Int? = null): List<LearningPathSummary>

    suspend fun getRecommendedPath(gradeLevel: Int): LearningPath?

    suspend fun getPath(pathId: String): LearningPath?

    suspend fun getState(userId: String, gradeLevel: Int? = null): LearningPathState

    suspend fun selectPath(userId: String, pathId: String): LearningPathState?

    suspend fun recordPathOpened(userId: String, pathId: String): LearningPathState?
}
