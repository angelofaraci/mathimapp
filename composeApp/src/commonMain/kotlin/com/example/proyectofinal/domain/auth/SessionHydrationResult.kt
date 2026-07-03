package com.example.proyectofinal.domain.auth

import com.example.proyectofinal.models.User

sealed interface SessionHydrationResult {
    data object Skipped : SessionHydrationResult

    data class Hydrated(val user: User) : SessionHydrationResult

    data object ClearedInvalidSession : SessionHydrationResult

    data class Failed(val message: String) : SessionHydrationResult
}
