package com.example.proyectofinal.seed

import com.example.proyectofinal.database.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.eq

object SeedData {
    private const val ADMIN_ID = "admin-001"
    private const val ADMIN_EMAIL = "admin@mathapp.com"
    private const val ADMIN_PASSWORD = "admin123"

    fun seedOfficialCourses() {
        transaction {
            val existingAdmin = Users.selectAll().where { Users.email eq ADMIN_EMAIL }.firstOrNull()
            if (existingAdmin != null) {
                println("Seed data already exists, skipping...")
                return@transaction
            }

            println("Seeding official courses...")

            Users.insert {
                it[Users.id] = ADMIN_ID
                it[Users.name] = "Admin"
                it[Users.email] = ADMIN_EMAIL
                it[Users.passwordHash] = "bcrypt_hash_placeholder"
                it[Users.role] = "ADMIN"
            }

            val basicArithmeticId = "course-basic-arithmetic"
            Courses.insert {
                it[Courses.id] = basicArithmeticId
                it[Courses.title] = "Basic Arithmetic"
                it[Courses.description] = "Learn the fundamentals of addition, subtraction, multiplication, and division."
                it[Courses.creatorId] = ADMIN_ID
                it[Courses.isOfficial] = true
                it[Courses.joinCode] = "ARITH101"
            }

            val additionLessonId = "lesson-addition"
            Lessons.insert {
                it[Lessons.id] = additionLessonId
                it[Lessons.courseId] = basicArithmeticId
                it[Lessons.title] = "Addition"
                it[Lessons.theoryContent] = """
                    # Introduction to Addition
                    
                    Addition is one of the fundamental operations in mathematics. 
                    When you add numbers, you combine them to get a total (sum).
                    
                    ## Example
                    2 + 3 = 5
                    
                    Here, 2 and 3 are called "addends" and 5 is the "sum".
                    
                    ## Properties of Addition
                    - **Commutative**: a + b = b + a
                    - **Associative**: (a + b) + c = a + (b + c)
                    - **Identity**: a + 0 = a
                """.trimIndent()
                it[Lessons.orderIndex] = 0
            }

            Exercises.insert {
                it[Exercises.id] = "ex-add-1"
                it[Exercises.lessonId] = additionLessonId
                it[Exercises.question] = "What is 5 + 3?"
                it[Exercises.options] = "6,7,8,9"
                it[Exercises.correctAnswer] = "8"
                it[Exercises.type] = "MULTIPLE_CHOICE"
            }

            Exercises.insert {
                it[Exercises.id] = "ex-add-2"
                it[Exercises.lessonId] = additionLessonId
                it[Exercises.question] = "What is 12 + 8?"
                it[Exercises.options] = "18,19,20,21"
                it[Exercises.correctAnswer] = "20"
                it[Exercises.type] = "MULTIPLE_CHOICE"
            }

            Exercises.insert {
                it[Exercises.id] = "ex-add-3"
                it[Exercises.lessonId] = additionLessonId
                it[Exercises.question] = "Is 4 + 4 = 8?"
                it[Exercises.options] = "True,False"
                it[Exercises.correctAnswer] = "True"
                it[Exercises.type] = "TRUE_FALSE"
            }

            val subtractionLessonId = "lesson-subtraction"
            Lessons.insert {
                it[Lessons.id] = subtractionLessonId
                it[Lessons.courseId] = basicArithmeticId
                it[Lessons.title] = "Subtraction"
                it[Lessons.theoryContent] = """
                    # Introduction to Subtraction
                    
                    Subtraction is the opposite of addition. When you subtract, 
                    you find the difference between two numbers.
                    
                    ## Example
                    7 - 3 = 4
                    
                    Here, 7 is the "minuend", 3 is the "subtrahend", and 4 is the "difference".
                    
                    ## Important Note
                    In subtraction, order matters! a - b is not the same as b - a.
                """.trimIndent()
                it[Lessons.orderIndex] = 1
            }

            Exercises.insert {
                it[Exercises.id] = "ex-sub-1"
                it[Exercises.lessonId] = subtractionLessonId
                it[Exercises.question] = "What is 10 - 4?"
                it[Exercises.options] = "4,5,6,7"
                it[Exercises.correctAnswer] = "6"
                it[Exercises.type] = "MULTIPLE_CHOICE"
            }

            val multiplicationLessonId = "lesson-multiplication"
            Lessons.insert {
                it[Lessons.id] = multiplicationLessonId
                it[Lessons.courseId] = basicArithmeticId
                it[Lessons.title] = "Multiplication"
                it[Lessons.theoryContent] = """
                    # Introduction to Multiplication
                    
                    Multiplication is repeated addition. Instead of adding the same number 
                    multiple times, we multiply.
                    
                    ## Example
                    3 x 4 = 12 (same as 4 + 4 + 4 = 12)
                    
                    ## Properties of Multiplication
                    - **Commutative**: a × b = b × a
                    - **Associative**: (a × b) × c = a × (b × c)
                    - **Identity**: a × 1 = a
                    - **Zero Property**: a × 0 = 0
                """.trimIndent()
                it[Lessons.orderIndex] = 2
            }

            Exercises.insert {
                it[Exercises.id] = "ex-mul-1"
                it[Exercises.lessonId] = multiplicationLessonId
                it[Exercises.question] = "What is 6 × 7?"
                it[Exercises.options] = "36,40,42,48"
                it[Exercises.correctAnswer] = "42"
                it[Exercises.type] = "MULTIPLE_CHOICE"
            }

            val divisionLessonId = "lesson-division"
            Lessons.insert {
                it[Lessons.id] = divisionLessonId
                it[Lessons.courseId] = basicArithmeticId
                it[Lessons.title] = "Division"
                it[Lessons.theoryContent] = """
                    # Introduction to Division
                    
                    Division is the opposite of multiplication. It tells us how many times 
                    one number fits into another.
                    
                    ## Example
                    12 ÷ 3 = 4
                    
                    Here, 12 is the "dividend", 3 is the "divisor", and 4 is the "quotient".
                    
                    ## Important Note
                    You cannot divide by zero!
                """.trimIndent()
                it[Lessons.orderIndex] = 3
            }

            Exercises.insert {
                it[Exercises.id] = "ex-div-1"
                it[Exercises.lessonId] = divisionLessonId
                it[Exercises.question] = "What is 20 ÷ 4?"
                it[Exercises.options] = "4,5,6,7"
                it[Exercises.correctAnswer] = "5"
                it[Exercises.type] = "MULTIPLE_CHOICE"
            }

            println("Seed data created successfully!")
            println("Admin credentials: email=$ADMIN_EMAIL, password=$ADMIN_PASSWORD")
        }
    }
}
