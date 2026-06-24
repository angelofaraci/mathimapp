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
    fun `register with empty fields shows required error without calling repository`() =
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
    fun `register sends only name email and password and authenticates student session`() =
        runTest(dispatcher) {
            val user = User("2", "Bob", "bob@example.com", UserRole.STUDENT)
            val repo = FakeAuthRepository().apply { registerResult = Result.success(user) }
            val vm = RegisterViewModel(repo)

            vm.onNameChange("Bob")
            vm.onEmailChange("bob@example.com")
            vm.onPasswordChange("top-secret")
            vm.register()

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

            vm.onNameChange("Taken")
            vm.onEmailChange("taken@example.com")
            vm.onPasswordChange("secret")
            vm.register()

            repo.completeRegister()
            advanceUntilIdle()

            assertEquals("Email already registered", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(repo.session.value.user)
            assertFalse(repo.session.value.isAuthenticated)
        }
}