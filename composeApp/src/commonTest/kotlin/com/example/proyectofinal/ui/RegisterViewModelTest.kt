package com.example.proyectofinal.ui

import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register with empty fields retains required error without calling repository`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository()
            val vm = RegisterViewModel(repo)

            vm.register()

            assertEquals("Name, email, and password are required", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(repo.registerCalls.isEmpty())
            assertFalse(repo.session.value.isAuthenticated)
        }

    @Test
    fun `continue validates each wizard step before advancing`() = runTest(dispatcher) {
        val vm = RegisterViewModel(FakeAuthRepository())

        vm.continueStep()
        assertEquals(1, vm.uiState.value.step)
        assertEquals("Ingresá tu nombre.", vm.uiState.value.fieldErrors[RegisterField.Name])

        vm.onNameChange("Bob")
        vm.continueStep()
        assertEquals(2, vm.uiState.value.step)

        vm.onEmailChange("invalid")
        vm.continueStep()
        assertEquals(2, vm.uiState.value.step)
        assertEquals("Ingresá un correo electrónico válido.", vm.uiState.value.fieldErrors[RegisterField.Email])
        assertEquals("Ingresá una contraseña.", vm.uiState.value.fieldErrors[RegisterField.Password])
    }

    @Test
    fun `back from later steps preserves data and back from step one resets state`() = runTest(dispatcher) {
        val vm = RegisterViewModel(FakeAuthRepository())

        vm.onNameChange("Bob")
        vm.continueStep()
        vm.onEmailChange("bob@example.com")
        vm.onPasswordChange("Secret1!")
        vm.continueStep()
        assertEquals(3, vm.uiState.value.step)

        assertFalse(vm.goBack())
        assertEquals(2, vm.uiState.value.step)
        assertEquals("Bob", vm.uiState.value.name)
        assertEquals("bob@example.com", vm.uiState.value.email)
        assertEquals("Secret1!", vm.uiState.value.password)

        assertFalse(vm.goBack())
        assertEquals(1, vm.uiState.value.step)
        assertTrue(vm.goBack())
        assertEquals(RegisterUiState(), vm.uiState.value)
    }

    @Test
    fun `password visibility and strength are deterministic`() = runTest(dispatcher) {
        val vm = RegisterViewModel(FakeAuthRepository())

        vm.onPasswordChange("short")
        assertEquals(PasswordStrength.Weak, vm.uiState.value.passwordStrength)
        vm.onPasswordChange("abc123")
        assertEquals(PasswordStrength.Medium, vm.uiState.value.passwordStrength)
        vm.onPasswordChange("Abcdef12!")
        assertEquals(PasswordStrength.Strong, vm.uiState.value.passwordStrength)

        vm.togglePasswordVisibility()
        assertTrue(vm.uiState.value.isPasswordVisible)
    }

    @Test
    fun `final step requires accepted terms and does not submit while unchecked`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = RegisterViewModel(repo)
        advanceToFinalStep(vm)

        vm.continueStep()

        assertEquals(3, vm.uiState.value.step)
        assertEquals(
            "Aceptá los términos y condiciones para continuar.",
            vm.uiState.value.fieldErrors[RegisterField.Terms]
        )
        assertTrue(repo.registerCalls.isEmpty())
    }

    @Test
    fun `register sends only name email and password and authenticates student session`() =
        runTest(dispatcher) {
            val user = User("2", "Bob", "bob@example.com", UserRole.STUDENT)
            val repo = FakeAuthRepository().apply { registerResult = Result.success(user) }
            val vm = RegisterViewModel(repo)
            advanceToFinalStep(vm)
            vm.setAcceptedTerms(true)

            vm.continueStep()

            assertTrue(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            // No role is ever passed: the call shape contains only name/email/password.
            assertEquals(
                listOf(Triple("Bob", "bob@example.com", "top-secret")),
                repo.registerCalls
            )

            repo.completeRegister()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            assertEquals(UserRole.STUDENT, repo.session.value.user?.role)
            assertTrue(repo.session.value.isAuthenticated)
        }

    @Test
    fun `register surfaces raw repository error message and keeps session anonymous`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository().apply {
                registerResult = Result.failure(RuntimeException("Email already registered"))
            }
            val vm = RegisterViewModel(repo)
            advanceToFinalStep(vm)
            vm.setAcceptedTerms(true)

            vm.continueStep()
            repo.completeRegister()
            advanceUntilIdle()

            assertEquals("Email already registered", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(repo.session.value.user)
            assertFalse(repo.session.value.isAuthenticated)
        }

    private fun advanceToFinalStep(vm: RegisterViewModel) {
        vm.onNameChange("Bob")
        vm.continueStep()
        vm.onEmailChange("bob@example.com")
        vm.onPasswordChange("top-secret")
        vm.continueStep()
        assertEquals(3, vm.uiState.value.step)
    }
}
