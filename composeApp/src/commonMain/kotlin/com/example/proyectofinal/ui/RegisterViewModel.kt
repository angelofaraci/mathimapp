package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Screen-local state for the public registration form. There is intentionally no
 * role field: public registration always creates a STUDENT account.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun register() {
        val current = _uiState.value
        if (current.name.isBlank() || current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "Name, email, and password are required"
            )
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = authRepository.register(
                name = current.name.trim(),
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
            // observes to replace this screen with onboarding or courses depending on
            // learner-profile completion. Reset loading state here so a failed-then-retry
            // path keeps the UI consistent.
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            }
        }
    }
}
