package com.example.proyectofinal.di

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

private val requiredCourseDiscoveryColumns = setOf(
    "topic",
    "difficulty",
    "durationMinutes",
    "xpReward"
)

internal fun SqlDriver.applyPendingLocalSchemaFixes(): SqlDriver {
    ensureCourseDiscoveryColumns()
    ensureLessonEntityShape()
    return this
}

private fun SqlDriver.ensureCourseDiscoveryColumns() {
    val existingColumns = executeQuery(
        identifier = null,
        sql = "PRAGMA table_info(CourseEntity)",
        mapper = { cursor ->
            val columns = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(1)?.let(columns::add)
            }
            QueryResult.Value(columns)
        },
        parameters = 0,
    ).value

    if (existingColumns.containsAll(requiredCourseDiscoveryColumns)) {
        return
    }

    if ("topic" !in existingColumns) {
        execute(
            identifier = null,
            sql = "ALTER TABLE CourseEntity ADD COLUMN topic TEXT",
            parameters = 0,
        ).value
    }

    if ("difficulty" !in existingColumns) {
        execute(
            identifier = null,
            sql = "ALTER TABLE CourseEntity ADD COLUMN difficulty TEXT",
            parameters = 0,
        ).value
    }

    if ("durationMinutes" !in existingColumns) {
        execute(
            identifier = null,
            sql = "ALTER TABLE CourseEntity ADD COLUMN durationMinutes INTEGER",
            parameters = 0,
        ).value
    }

    if ("xpReward" !in existingColumns) {
        execute(
            identifier = null,
            sql = "ALTER TABLE CourseEntity ADD COLUMN xpReward INTEGER",
            parameters = 0,
        ).value
    }
}

private fun SqlDriver.ensureLessonEntityShape() {
    val columns = executeQuery(
        identifier = null,
        sql = "PRAGMA table_info(LessonEntity)",
        mapper = { cursor ->
            val lessonColumns = mutableMapOf<String, Boolean>()
            while (cursor.next().value) {
                val name = cursor.getString(1) ?: continue
                val isNotNull = cursor.getLong(3) == 1L
                lessonColumns[name] = isNotNull
            }
            QueryResult.Value(lessonColumns)
        },
        parameters = 0,
    ).value

    if (columns.isEmpty()) {
        return
    }

    val missingCreatorId = "creatorId" !in columns
    val courseIdIsStillRequired = columns["courseId"] == true

    if (!missingCreatorId && !courseIdIsStillRequired) {
        return
    }

    execute(identifier = null, sql = "PRAGMA foreign_keys=OFF", parameters = 0).value
    execute(identifier = null, sql = "BEGIN TRANSACTION", parameters = 0).value

    try {
        execute(
            identifier = null,
            sql = """
                CREATE TABLE LessonEntity_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    courseId TEXT,
                    creatorId TEXT,
                    title TEXT NOT NULL,
                    theoryContent TEXT NOT NULL,
                    FOREIGN KEY(courseId) REFERENCES CourseEntity(id) ON DELETE CASCADE
                )
            """.trimIndent(),
            parameters = 0,
        ).value

        val creatorIdSelect = if (missingCreatorId) "NULL" else "creatorId"
        execute(
            identifier = null,
            sql = """
                INSERT INTO LessonEntity_new (id, courseId, creatorId, title, theoryContent)
                SELECT id, courseId, $creatorIdSelect, title, theoryContent
                FROM LessonEntity
            """.trimIndent(),
            parameters = 0,
        ).value

        execute(identifier = null, sql = "DROP TABLE LessonEntity", parameters = 0).value
        execute(
            identifier = null,
            sql = "ALTER TABLE LessonEntity_new RENAME TO LessonEntity",
            parameters = 0,
        ).value
        execute(identifier = null, sql = "COMMIT TRANSACTION", parameters = 0).value
    } catch (error: Throwable) {
        execute(identifier = null, sql = "ROLLBACK TRANSACTION", parameters = 0).value
        throw error
    } finally {
        execute(identifier = null, sql = "PRAGMA foreign_keys=ON", parameters = 0).value
    }
}
