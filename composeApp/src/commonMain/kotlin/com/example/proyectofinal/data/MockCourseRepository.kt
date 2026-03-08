package com.example.proyectofinal.data

import com.example.proyectofinal.domain.*

class MockCourseRepository : CourseRepository {
    private val mockCourses = listOf(
        Course(
            id = "math-101",
            title = "Basic Arithmetic",
            description = "Master addition, subtraction, multiplication, and division.",
            lessons = listOf(
                Lesson(
                    id = "lesson-1",
                    title = "Introduction to Addition",
                    theoryContent = "# Addition\n\nAddition is the process of calculating the total of two or more numbers.",
                    exercises = listOf(
                        Exercise(
                            id = "ex-1",
                            question = "What is 2 + 2?",
                            options = listOf("3", "4", "5"),
                            correctAnswer = "4",
                            type = ExerciseType.MULTIPLE_CHOICE
                        ),
                        Exercise(
                            id = "ex-2",
                            question = "What is 5 + 7?",
                            options = listOf("10", "12", "14"),
                            correctAnswer = "12",
                            type = ExerciseType.MULTIPLE_CHOICE
                        )
                    )
                ),
                Lesson(
                    id = "lesson-2",
                    title = "Basic Subtraction",
                    theoryContent = "# Subtraction\n\nSubtraction is taking one number away from another.",
                    exercises = listOf(
                        Exercise(
                            id = "ex-3",
                            question = "What is 10 - 4?",
                            options = listOf("5", "6", "7"),
                            correctAnswer = "6",
                            type = ExerciseType.MULTIPLE_CHOICE
                        )
                    )
                )
            )
        ),
        Course(
            id = "algebra-1",
            title = "Introduction to Algebra",
            description = "Learn about variables and simple equations.",
            lessons = emptyList()
        )
    )

    private var userProgress = UserProgress(userId = "default-user")

    override suspend fun getCourses(): List<Course> = mockCourses

    override suspend fun getCourseById(id: String): Course? {
        return mockCourses.find { it.id == id }
    }

    override suspend fun getUserProgress(userId: String): UserProgress = userProgress

    override suspend fun saveProgress(progress: UserProgress) {
        userProgress = progress
    }
}
