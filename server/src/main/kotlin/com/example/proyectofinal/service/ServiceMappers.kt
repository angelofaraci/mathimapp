package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseType
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
        schoolYear = this[Courses.schoolYear]
    )

internal fun ResultRow.toLesson(exercises: List<Exercise> = emptyList()): Lesson =
    Lesson(
        id = this[Lessons.id],
        courseId = this[Lessons.courseId],
        title = this[Lessons.title],
        theoryContent = this[Lessons.theoryContent],
        exercises = exercises
    )

internal fun ResultRow.toExercise(hideAnswers: Boolean = false): Exercise =
    Exercise(
        id = this[Exercises.id],
        lessonId = this[Exercises.lessonId],
        question = this[Exercises.question],
        options = this[Exercises.options].split(","),
        correctAnswer = if (hideAnswers) "" else this[Exercises.correctAnswer],
        type = ExerciseType.valueOf(this[Exercises.type])
    )

internal fun ResultRow.toUser(): User =
    User(
        id = this[Users.id],
        name = this[Users.name],
        email = this[Users.email],
        role = UserRole.valueOf(this[Users.role])
    )
