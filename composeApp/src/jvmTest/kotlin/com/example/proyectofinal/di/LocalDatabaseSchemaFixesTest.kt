package com.example.proyectofinal.di

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDatabaseSchemaFixesTest {
    @Test
    fun `repair step upgrades persisted course rows before AppDatabase opens`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE CourseEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    creatorId TEXT NOT NULL,
                    isOfficial INTEGER NOT NULL DEFAULT 0,
                    schoolYear INTEGER NOT NULL DEFAULT 0,
                    joinCode TEXT
                )
            """.trimIndent(),
            parameters = 0,
        ).value

        driver.applyPendingLocalSchemaFixes()

        val intAdapter = object : ColumnAdapter<Int, Long> {
            override fun decode(databaseValue: Long): Int = databaseValue.toInt()

            override fun encode(value: Int): Long = value.toLong()
        }

        val database = AppDatabase(
            driver = driver,
            CourseEntityAdapter = CourseEntity.Adapter(
                schoolYearAdapter = intAdapter,
                durationMinutesAdapter = intAdapter,
                xpRewardAdapter = intAdapter,
            ),
            ExerciseEntityAdapter = ExerciseEntity.Adapter(
                typeAdapter = EnumColumnAdapter(),
            ),
            UserProgressEntityAdapter = UserProgressEntity.Adapter(
                totalScoreAdapter = intAdapter,
            ),
            UserEntityAdapter = UserEntity.Adapter(
                roleAdapter = userRoleColumnAdapter,
            ),
        )

        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Fractions Basics",
            description = "Learn equivalent fractions",
            creatorId = "admin",
            isOfficial = true,
            schoolYear = 4,
            joinCode = null,
            topic = "Fracciones",
            difficulty = "Fácil",
            durationMinutes = 15,
            xpReward = 50,
        )

        val upgradedCourse = database.appDatabaseQueries.selectCourseById("course-1").executeAsOne()

        assertEquals("Fracciones", upgradedCourse.topic)
        assertEquals("Fácil", upgradedCourse.difficulty)
        assertEquals(15, upgradedCourse.durationMinutes)
        assertEquals(50, upgradedCourse.xpReward)
    }
}
