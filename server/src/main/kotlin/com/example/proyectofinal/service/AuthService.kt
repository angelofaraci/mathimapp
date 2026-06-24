package com.example.proyectofinal.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.RegisterRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class AuthService {
    fun findUserByEmail(email: String): AuthUserRecord? = dbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .firstOrNull()
            ?.toAuthUserRecord()
    }

    fun createUser(userId: String, request: RegisterRequest, passwordHash: String): User = dbQuery {
        Users.insert {
            it[Users.id] = userId
            it[Users.name] = request.name
            it[Users.email] = request.email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = UserRole.STUDENT.name
        }

        User(
            id = userId,
            name = request.name,
            email = request.email,
            role = UserRole.STUDENT
        )
    }

    fun validateCredentials(email: String, password: String): User? = dbQuery {
        val user = Users.selectAll()
            .where { Users.email eq email }
            .firstOrNull()
            ?.toAuthUserRecord()
            ?: return@dbQuery null

        val passwordMatch = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        if (!passwordMatch.verified) {
            return@dbQuery null
        }

        user.toUser()
    }

    data class AuthUserRecord(
        val id: String,
        val name: String,
        val email: String,
        val passwordHash: String,
        val role: String
    ) {
        fun toUser(): User = User(
            id = id,
            name = name,
            email = email,
            role = UserRole.parse(role)
                ?: error("Unknown stored auth user role: $role")
        )
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toAuthUserRecord(): AuthUserRecord =
        AuthUserRecord(
            id = this[Users.id],
            name = this[Users.name],
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            role = this[Users.role]
        )
}
