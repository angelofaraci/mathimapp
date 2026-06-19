package com.example.proyectofinal.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.*
import com.example.proyectofinal.plugins.Security
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.eq
import java.util.*

fun Application.authRoutes() {
    routing {
        post("/auth/register") {
            val request = call.receive<RegisterRequest>()

            val existingUser = dbQuery {
                Users.selectAll().where { Users.email eq request.email }.firstOrNull()
            }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, "Email already registered")
                return@post
            }

            val userId = UUID.randomUUID().toString()
            val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

            dbQuery {
                Users.insert {
                    it[Users.id] = userId
                    it[Users.name] = request.name
                    it[Users.email] = request.email
                    it[Users.passwordHash] = passwordHash
                    it[Users.role] = request.role.name
                }
            }

            val user = User(
                id = userId,
                name = request.name,
                email = request.email,
                role = request.role
            )

            val token = Security.generateToken(userId, request.role.name)

            call.respond(AuthResponse(token = token, user = user))
        }

        post("/auth/login") {
            val request = call.receive<LoginRequest>()

            val userRow = dbQuery {
                Users.selectAll().where { Users.email eq request.email }.firstOrNull()
            }
            if (userRow == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
                return@post
            }

            val storedHash = userRow[Users.passwordHash]
            val passwordMatch = BCrypt.verifyer().verify(request.password.toCharArray(), storedHash)
            if (!passwordMatch.verified) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid email or password")
                return@post
            }

            val userId = userRow[Users.id]
            val name = userRow[Users.name]
            val email = userRow[Users.email]
            val role = UserRole.valueOf(userRow[Users.role])

            val user = User(id = userId, name = name, email = email, role = role)
            val token = Security.generateToken(userId, role.name)

            call.respond(AuthResponse(token = token, user = user))
        }
    }
}
