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
