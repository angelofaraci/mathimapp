package com.example.proyectofinal.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.example.proyectofinal.models.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

object Security {
    internal const val ISSUER = "ktor-math-app"
    private const val SECRET_ENV = "JWT_SECRET"
    private const val SECRET_PROPERTY = "jwt.secret"
    private const val VALIDITY_IN_MS = 24 * 60 * 60 * 1000L // 24 hours

    private fun configuredSecret(): String =
        System.getenv(SECRET_ENV)?.takeIf { it.isNotBlank() }
            ?: System.getProperty(SECRET_PROPERTY)?.takeIf { it.isNotBlank() }
            ?: error("JWT secret must be configured via $SECRET_ENV or -D$SECRET_PROPERTY")

    private val algorithm by lazy { Algorithm.HMAC256(configuredSecret()) }

    val verifier: JWTVerifier by lazy {
        JWT.require(algorithm)
        .withIssuer(ISSUER)
        .build()
    }

    fun generateToken(userId: String, role: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
            .sign(algorithm)
    }

    data class JWTPayload(val userId: String, val role: String)
}

fun ApplicationCall.currentUserId(): String? =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asString()
        ?.takeIf { it.isNotBlank() }

fun ApplicationCall.currentRole(): UserRole? =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("role")
        ?.asString()
        ?.takeIf { it.isNotBlank() }
        ?.let(UserRole::parse)

private suspend fun ApplicationCall.respondAuthFailure(): Boolean {
    respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
    return false
}

suspend fun ApplicationCall.requireSelfOrAdmin(targetUserId: String): Boolean {
    val authenticatedUserId = currentUserId() ?: return respondAuthFailure()
    val role = currentRole() ?: return respondAuthFailure()

    if (authenticatedUserId != targetUserId && role != UserRole.ADMIN) {
        respond(HttpStatusCode.Forbidden, "Forbidden")
        return false
    }

    return true
}

suspend fun ApplicationCall.requireAdmin(): Boolean {
    val role = currentRole() ?: return respondAuthFailure()

    if (role != UserRole.ADMIN) {
        respond(HttpStatusCode.Forbidden, "Forbidden")
        return false
    }

    return true
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = Security.ISSUER
            verifier(Security.verifier)
            validate { credential ->
                if (credential.payload.getClaim("userId").asString()?.isNotBlank() == true) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            }
        }
    }
}
