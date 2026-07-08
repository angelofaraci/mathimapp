package com.example.proyectofinal.di

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.example.proyectofinal.data.ExercisePayloadJson

private val requiredCourseDiscoveryColumns = setOf(
    "topic",
    "difficulty",
    "durationMinutes",
    "xpReward"
)

internal fun SqlDriver.applyPendingLocalSchemaFixes(): SqlDriver {
    ensureCourseDiscoveryColumns()
    ensureLessonEntityShape()
    ensureExerciseEntityShape()
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

private data class PersistedExerciseRow(
    val id: String,
    val lessonId: String,
    val title: String,
    val typeName: String,
    val payloadJson: String,
)

private fun SqlDriver.ensureExerciseEntityShape() {
    val columns = executeQuery(
        identifier = null,
        sql = "PRAGMA table_info(ExerciseEntity)",
        mapper = { cursor ->
            val exerciseColumns = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(1)?.let(exerciseColumns::add)
            }
            QueryResult.Value(exerciseColumns)
        },
        parameters = 0,
    ).value

    if (columns.isEmpty()) {
        return
    }

    val hasTitle = "title" in columns
    val hasPayload = "payload" in columns
    val hasLegacyQuestion = "question" in columns
    val hasLegacyOptions = "options" in columns
    val hasLegacyCorrectAnswer = "correctAnswer" in columns

    if (hasTitle && hasPayload && !hasLegacyQuestion && !hasLegacyOptions && !hasLegacyCorrectAnswer) {
        return
    }

    val persistedRows = readPersistedExercises(
        hasTitle = hasTitle,
        hasPayload = hasPayload,
    )

    execute(identifier = null, sql = "PRAGMA foreign_keys=OFF", parameters = 0).value
    execute(identifier = null, sql = "BEGIN TRANSACTION", parameters = 0).value

    try {
        execute(
            identifier = null,
            sql = """
                CREATE TABLE ExerciseEntity_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    lessonId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'MULTIPLE_CHOICE',
                    payload TEXT NOT NULL,
                    FOREIGN KEY(lessonId) REFERENCES LessonEntity(id) ON DELETE CASCADE
                )
            """.trimIndent(),
            parameters = 0,
        ).value

        persistedRows.forEach { row ->
            execute(
                identifier = null,
                sql = """
                    INSERT INTO ExerciseEntity_new (id, lessonId, title, type, payload)
                    VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                parameters = 5,
            ) {
                bindString(0, row.id)
                bindString(1, row.lessonId)
                bindString(2, row.title)
                bindString(3, row.typeName)
                bindString(4, row.payloadJson)
            }.value
        }

        execute(identifier = null, sql = "DROP TABLE ExerciseEntity", parameters = 0).value
        execute(
            identifier = null,
            sql = "ALTER TABLE ExerciseEntity_new RENAME TO ExerciseEntity",
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

private fun SqlDriver.readPersistedExercises(
    hasTitle: Boolean,
    hasPayload: Boolean,
): List<PersistedExerciseRow> {
    val selectSql = buildString {
        append("SELECT id, lessonId, ")
        append(if (hasTitle) "title" else "question")
        append(", type, ")
        if (hasPayload) {
            append("payload")
        } else {
            append("options, correctAnswer")
        }
        append(" FROM ExerciseEntity")
    }

    return executeQuery(
        identifier = null,
        sql = selectSql,
        mapper = { cursor ->
            val rows = mutableListOf<PersistedExerciseRow>()
            while (cursor.next().value) {
                val rawType = cursor.getString(3) ?: "MULTIPLE_CHOICE"
                val type = ExercisePayloadJson.parseType(rawType)
                val payloadJson = if (hasPayload) {
                    cursor.getString(4).orEmpty()
                } else {
                    ExercisePayloadJson.legacyPayloadJson(
                        type = type,
                        optionsCsv = cursor.getString(4).orEmpty(),
                        correctAnswer = cursor.getString(5).orEmpty(),
                    )
                }

                rows += PersistedExerciseRow(
                    id = cursor.getString(0).orEmpty(),
                    lessonId = cursor.getString(1).orEmpty(),
                    title = cursor.getString(2).orEmpty(),
                    typeName = type.name,
                    payloadJson = payloadJson,
                )
            }
            QueryResult.Value(rows)
        },
        parameters = 0,
    ).value
}
