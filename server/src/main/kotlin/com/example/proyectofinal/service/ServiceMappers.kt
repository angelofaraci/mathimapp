package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toCourse(lessons: List<Lesson> = emptyList()): Course =
    Course(
        id = this[Courses.id],
        title = this[Courses.title],
        description = this[Courses.description],
        creatorId = this[Courses.creatorId],
        isOfficial = this[Courses.isOfficial],
        joinCode = this[Courses.joinCode],
        lessons = lessons,
        schoolYear = this[Courses.schoolYear],
        topic = this[Courses.topic],
        difficulty = this[Courses.difficulty],
        durationMinutes = this[Courses.durationMinutes],
        xpReward = this[Courses.xpReward]
    )

internal fun ResultRow.toLesson(exercises: List<Exercise> = emptyList()): Lesson =
    Lesson(
        id = this[Lessons.id],
        courseId = this[Lessons.courseId],
        creatorId = this[Lessons.creatorId],
        title = this[Lessons.title],
        theoryContent = this[Lessons.theoryContent],
        exercises = exercises
    )

internal fun ResultRow.toExercise(hideAnswers: Boolean = false): Exercise =
    ExercisePayloadSupport.toExercise(
        id = this[Exercises.id],
        lessonId = this[Exercises.lessonId],
        title = this[Exercises.question],
        persistedType = this[Exercises.type],
        persistedPayload = this[Exercises.payload],
        legacyOptions = this[Exercises.options],
        legacyCorrectAnswer = this[Exercises.correctAnswer],
        hideAnswers = hideAnswers
    )

internal fun ResultRow.toUser(): User =
    User(
        id = this[Users.id],
        name = this[Users.name],
        email = this[Users.email],
        role = UserRole.parse(this[Users.role])
            ?: error("Unknown persisted user role: ${this[Users.role]}")
    )
