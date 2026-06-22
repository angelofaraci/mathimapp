package com.example.proyectofinal.database

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database

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
        migrate(
            url = url,
            driver = driver,
            user = user,
            password = password
        )

        Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password
        )
    }

    private fun migrate(
        url: String,
        driver: String,
        user: String,
        password: String
    ) {
        Class.forName(driver)

        Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .load()
            .migrate()
    }

    private fun env(name: String, defaultValue: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue
}
