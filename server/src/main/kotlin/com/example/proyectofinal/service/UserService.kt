package com.example.proyectofinal.service

import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.UserProgress as UserProgressTable
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.UpdateUserRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class UserService {
    fun getUserById(id: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .firstOrNull()
            ?.toUser()
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
        val progressRow = UserProgressTable.selectAll()
            .where { UserProgressTable.userId eq userId }
            .firstOrNull()
        val completedLessons = CompletedLessons.selectAll()
            .where { CompletedLessons.userId eq userId }
            .map { it[CompletedLessons.lessonId] }
            .toSet()
        val enrolledCourses = EnrolledCourses.selectAll()
            .where { EnrolledCourses.userId eq userId }
            .map { it[EnrolledCourses.courseId] }
            .toSet()

        UserProgress(
            userId = userId,
            totalScore = progressRow?.get(UserProgressTable.totalScore) ?: 0,
            completedLessonIds = completedLessons,
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
}
