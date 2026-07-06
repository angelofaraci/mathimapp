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
import kotlin.test.assertNull

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

    @Test
    fun `repair step upgrades persisted lesson rows before AppDatabase opens`() {
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
                    joinCode TEXT,
                    topic TEXT,
                    difficulty TEXT,
                    durationMinutes INTEGER,
                    xpReward INTEGER
                )
            """.trimIndent(),
            parameters = 0,
        ).value
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE LessonEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    courseId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    theoryContent TEXT NOT NULL,
                    FOREIGN KEY(courseId) REFERENCES CourseEntity(id) ON DELETE CASCADE
                )
            """.trimIndent(),
            parameters = 0,
        ).value
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO CourseEntity(id, title, description, creatorId, isOfficial, schoolYear, joinCode, topic, difficulty, durationMinutes, xpReward)
                VALUES ('course-1', 'Fractions Basics', 'Learn equivalent fractions', 'admin', 1, 4, NULL, 'Fractions', 'Easy', 15, 50)
            """.trimIndent(),
            parameters = 0,
        ).value
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO LessonEntity(id, courseId, title, theoryContent)
                VALUES ('lesson-1', 'course-1', 'Equivalent fractions', 'Theory')
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

        val upgradedLesson = database.appDatabaseQueries.selectLessonById("lesson-1").executeAsOne()
        assertEquals("course-1", upgradedLesson.courseId)
        assertNull(upgradedLesson.creatorId)

        database.appDatabaseQueries.insertLesson(
            id = "lesson-2",
            courseId = null,
            creatorId = "teacher-1",
            title = "Practice",
            theoryContent = "Practice theory",
        )

        val insertedLesson = database.appDatabaseQueries.selectLessonById("lesson-2").executeAsOne()
        assertNull(insertedLesson.courseId)
        assertEquals("teacher-1", insertedLesson.creatorId)
    }
}
