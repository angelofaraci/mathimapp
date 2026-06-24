package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Screen-local state for the login form. It only carries UI-facing data and
 * MUST NOT store tokens; token/session mutation lives in [AuthRepository].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun login() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Email and password are required")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.login(
                email = current.email.trim(),
                password = current.password
            )

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error"
                )
            }

            // On success the AuthRepository flips its session StateFlow, which App.kt
            // observes to replace this screen with CourseScreen. Reset loading state
            // here so a failed-then-retry path keeps the UI consistent.
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }
}