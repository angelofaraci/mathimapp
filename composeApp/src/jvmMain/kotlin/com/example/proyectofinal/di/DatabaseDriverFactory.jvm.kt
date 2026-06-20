package com.example.proyectofinal.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.proyectofinal.db.AppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite:composeApp.db",
        schema = AppDatabase.Schema,
        migrateEmptySchema = true
    )
}
