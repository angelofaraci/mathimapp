package com.example.proyectofinal.ui

import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runtime coverage for the `frontend-auth` "Auth Entry Flow" scenarios that the
 * verify gate flagged as untested. The routing decision lives in the pure
 * [AuthGateRouter] / [resolveAuthView] construct, including the authenticated
 * dashboard landing path, so these are plain kotlin.test + StateFlow
 * assertions and need no Compose UI test harness.
 */
class AuthGateRoutingTest {

    // Scenario: "Default state is login" — default target is LOGIN, Register not selected.
    @Test
    fun `default target is login and register is not selected`() {
        val router = AuthGateRouter()

        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
    }

    // Scenario: "Default state is login" — GIVEN app starts with no token,
    // THEN Login screen visible, Register NOT shown.
    @Test
    fun `default view is login when session is anonymous`() {
        val anonymous = AuthSession()
        val router = AuthGateRouter()

        val view = resolveAuthView(anonymous, router.target.value, onboardingComplete = false)

        assertEquals(AuthView.LOGIN, view)
    }

    // Scenario: "Text links switch forms" — GIVEN Login visible,
    // WHEN user selects the register link, THEN Register replaces it.
    @Test
    fun `selecting register link switches from login to register`() {
        val router = AuthGateRouter()
        assertEquals(
            AuthView.LOGIN,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )

        router.switchToRegister()

        assertEquals(AuthScreenTarget.REGISTER, router.target.value)
        assertEquals(
            AuthView.REGISTER,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )
    }

    // Scenario: "Text links switch forms" — the reverse direction (Register link back to Login).
    @Test
    fun `selecting login link switches back from register to login`() {
        val router = AuthGateRouter().apply { switchToRegister() }
        assertEquals(
            AuthView.REGISTER,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )

        router.switchToLogin()

        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
        assertEquals(
            AuthView.LOGIN,
            resolveAuthView(AuthSession(), router.target.value, onboardingComplete = false)
        )
    }

    @Test
    fun `toggle flips between login and register in both directions`() {
        val router = AuthGateRouter()

        router.toggle()
        assertEquals(AuthScreenTarget.REGISTER, router.target.value)

        router.toggle()
        assertEquals(AuthScreenTarget.LOGIN, router.target.value)
    }

    @Test
    fun `authenticated session with completed onboarding routes to dashboard landing`() {
        val authenticated = AuthSession(
            token = "token-123",
            user = User("1", "Alice", "alice@example.com", UserRole.STUDENT)
        )
        val router = AuthGateRouter().apply { switchToRegister() }

        assertEquals(
            AuthView.COURSE,
            resolveAuthView(authenticated, router.target.value, onboardingComplete = true)
        )
    }

    @Test
    fun `authenticated session with incomplete onboarding routes to onboarding`() {
        val authenticated = AuthSession(
            token = "token-123",
            user = User("1", "Alice", "alice@example.com", UserRole.STUDENT)
        )
        val router = AuthGateRouter().apply { switchToRegister() }

        assertEquals(
            AuthView.ONBOARDING,
            resolveAuthView(authenticated, router.target.value, onboardingComplete = false)
        )
    }
}
