package com.example.proyectofinal.database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init() {
        init(
            url = env("DB_URL", "jdbc:postgresql://localhost:5432/MathimApp"),
            driver = env("DB_DRIVER", "org.postgresql.Driver"),
            user = env("DB_USER", "postgres"),
            password = env("DB_PASSWORD", "mathimapp")
        )
    }

    fun init(
        url: String,
        driver: String,
        user: String,
        password: String
    ) {
        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )

        transaction {
            SchemaUtils.create(
                Users,
                Courses,
                Lessons,
                Exercises,
                UserProgress,
                CompletedLessons,
                EnrolledCourses
            )

            exec(
                """
                ALTER TABLE courses
                    ADD COLUMN IF NOT EXISTS school_year INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    private fun env(name: String, defaultValue: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue
}
