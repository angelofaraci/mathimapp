package com.example.proyectofinal.database

import org.jetbrains.exposed.v1.jdbc.transactions.transaction

inline fun <T> dbQuery(crossinline block: () -> T): T = transaction { block() }
