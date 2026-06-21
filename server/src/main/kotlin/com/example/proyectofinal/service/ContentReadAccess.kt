package com.example.proyectofinal.service

import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

internal data class CourseContentAccess(
    val courseId: String,
    val creatorId: String,
    val isOfficial: Boolean
)

internal fun canReadCourseContent(access: CourseContentAccess, userId: String, role: UserRole): Boolean =
    when (role) {
        UserRole.ADMIN -> true
        UserRole.TEACHER -> access.creatorId == userId
        UserRole.LEARNER -> access.isOfficial || isUserEnrolledInCourse(userId, access.courseId)
    }

private fun isUserEnrolledInCourse(userId: String, courseId: String): Boolean = dbQuery {
    EnrolledCourses.selectAll()
        .where {
            (EnrolledCourses.userId eq userId) and
                (EnrolledCourses.courseId eq courseId)
        }
        .count() > 0
}
