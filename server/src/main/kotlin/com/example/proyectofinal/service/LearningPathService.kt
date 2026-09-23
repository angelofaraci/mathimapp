package com.example.proyectofinal.service

import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.LearningPathDefaultGradeLevels
import com.example.proyectofinal.database.LearningPathLessons
import com.example.proyectofinal.database.LearningPaths
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.UserLearningPathState
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.LearningPath
import com.example.proyectofinal.models.LearningPathLesson
import com.example.proyectofinal.models.LearningPathObjective
import com.example.proyectofinal.models.LearningPathObjectiveType
import com.example.proyectofinal.models.LearningPathState
import com.example.proyectofinal.models.LearningPathSummary
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Read and server-side state management for platform-curated paths.
 *
 * A path is readable only when every linked lesson belongs to an official course.
 * This defensive check prevents a curation mistake from exposing private classroom content.
 */
class LearningPathService {
    fun listPathsForUser(userId: String, gradeLevel: Int? = null): List<LearningPathSummary> = dbQuery {
        LearningPaths.selectAll()
            .asSequence()
            .filter { gradeLevel == null || it[LearningPaths.objectiveGradeLevel] == gradeLevel }
            .mapNotNull { pathRow -> summaryForUser(pathRow[LearningPaths.id], userId) }
            .toList()
    }
    fun getPathForUser(pathId: String, userId: String): LearningPath? = dbQuery {
        val path = LearningPaths.selectAll()
            .where { LearningPaths.id eq pathId }
            .firstOrNull()
            ?: return@dbQuery null

        val membershipCount = LearningPathLessons.selectAll()
            .where { LearningPathLessons.pathId eq pathId }
            .count()
            .toInt()
        val completedLessonIds = completedLessonIds(userId)
        val lessons = officialLessonsForPath(pathId, completedLessonIds)

        if (lessons.size != membershipCount) {
            return@dbQuery null
        }

        LearningPath(
            id = path[LearningPaths.id],
            name = path[LearningPaths.name],
            description = path[LearningPaths.description],
            visibleObjective = path[LearningPaths.visibleObjective],
            objective = path.toObjective(),
            lessons = lessons
        )
    }

    fun resolveDefaultPathForUser(gradeLevel: Int, userId: String): LearningPath? = dbQuery {
        val pathId = LearningPathDefaultGradeLevels.selectAll()
            .where { LearningPathDefaultGradeLevels.gradeLevel eq gradeLevel }
            .firstOrNull()
            ?.get(LearningPathDefaultGradeLevels.pathId)
            ?: return@dbQuery null

        getPathForUser(pathId, userId)
    }

    fun getState(userId: String, recommendedGradeLevel: Int? = null): LearningPathState = dbQuery {
        val persisted = UserLearningPathState.selectAll()
            .where { UserLearningPathState.userId eq userId }
            .firstOrNull()
        val recommendedPathId = recommendedGradeLevel
            ?.let(::defaultPathIdForGradeLevel)
            ?.takeIf { isReadablePath(it) }

        LearningPathState(
            recommendedPathId = recommendedPathId,
            selectedPathId = persisted?.get(UserLearningPathState.selectedPathId)?.takeIf(::isReadablePath),
            lastOpenedPathId = persisted?.get(UserLearningPathState.lastOpenedPathId)?.takeIf(::isReadablePath)
        )
    }

    fun selectPath(userId: String, pathId: String): Boolean = updateState(
        userId = userId,
        pathId = pathId,
        updateSelectedPath = true
    )

    fun recordPathOpened(userId: String, pathId: String): Boolean = updateState(
        userId = userId,
        pathId = pathId,
        updateSelectedPath = false
    )

    private fun updateState(
        userId: String,
        pathId: String,
        updateSelectedPath: Boolean
    ): Boolean = dbQuery {
        if (!userExists(userId) || !isReadablePath(pathId)) {
            return@dbQuery false
        }

        val updated = UserLearningPathState.update({ UserLearningPathState.userId eq userId }) {
            if (updateSelectedPath) {
                it[UserLearningPathState.selectedPathId] = pathId
            } else {
                it[UserLearningPathState.lastOpenedPathId] = pathId
            }
        }
        if (updated == 0) {
            UserLearningPathState.insert {
                it[UserLearningPathState.userId] = userId
                if (updateSelectedPath) {
                    it[UserLearningPathState.selectedPathId] = pathId
                } else {
                    it[UserLearningPathState.lastOpenedPathId] = pathId
                }
            }
        }
        true
    }

    private fun summaryForUser(pathId: String, userId: String): LearningPathSummary? {
        val path = getPathForUser(pathId, userId) ?: return null
        val completedCount = path.lessons.count(LearningPathLesson::completed)
        val defaultPathId = defaultPathIdForGradeLevel(path.objective.gradeLevel)

        return LearningPathSummary(
            id = path.id,
            name = path.name,
            description = path.description,
            visibleObjective = path.visibleObjective,
            objective = path.objective,
            lessonCount = path.lessons.size,
            completedLessonCount = completedCount,
            progressPercentage = if (path.lessons.isEmpty()) 0 else completedCount * 100 / path.lessons.size,
            isDefaultForObjective = defaultPathId == path.id
        )
    }

    private fun officialLessonsForPath(pathId: String, completedLessonIds: Set<String>): List<LearningPathLesson> =
        LearningPathLessons
            .innerJoin(Lessons)
            .innerJoin(Courses)
            .selectAll()
            .where {
                (LearningPathLessons.pathId eq pathId) and
                    (Courses.isOfficial eq true)
            }
            .orderBy(LearningPathLessons.orderIndex)
            .map { row ->
                LearningPathLesson(
                    lessonId = row[Lessons.id],
                    title = row[Lessons.title],
                    orderIndex = row[LearningPathLessons.orderIndex],
                    completed = row[Lessons.id] in completedLessonIds
                )
            }

    private fun completedLessonIds(userId: String): Set<String> = CompletedLessons.selectAll()
        .where { CompletedLessons.userId eq userId }
        .map { it[CompletedLessons.lessonId] }
        .toSet()

    private fun isReadablePath(pathId: String): Boolean {
        val memberCount = LearningPathLessons.selectAll()
            .where { LearningPathLessons.pathId eq pathId }
            .count()
            .toInt()
        return LearningPaths.selectAll().where { LearningPaths.id eq pathId }.count() == 1L &&
            officialLessonsForPath(pathId, emptySet()).size == memberCount
    }

    private fun defaultPathIdForGradeLevel(gradeLevel: Int): String? =
        LearningPathDefaultGradeLevels.selectAll()
            .where { LearningPathDefaultGradeLevels.gradeLevel eq gradeLevel }
            .firstOrNull()
            ?.get(LearningPathDefaultGradeLevels.pathId)

    private fun userExists(userId: String): Boolean = Users.selectAll()
        .where { Users.id eq userId }
        .count() == 1L

    private fun org.jetbrains.exposed.v1.core.ResultRow.toObjective(): LearningPathObjective {
        check(this[LearningPaths.objectiveType] == LearningPathObjectiveType.GRADE_LEVEL.name) {
            "Unsupported learning path objective type: ${this[LearningPaths.objectiveType]}"
        }
        return LearningPathObjective(gradeLevel = this[LearningPaths.objectiveGradeLevel])
    }
}
