package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.models.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: CourseRepository,
    private val learnerProfileRepository: LearnerProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourseUiState>(CourseUiState.Loading)
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = CourseUiState.Loading
            try {
                val userId = authRepository.session.value.user?.id.orEmpty()
                val schoolYear = learnerProfileRepository.getProfile(userId)?.schoolYear
                val courses = repository.getOfficialCourses(schoolYear)
                _uiState.value = CourseUiState.Success(courses)
            } catch (e: Exception) {
                _uiState.value = CourseUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface CourseUiState {
    data object Loading : CourseUiState
    data class Success(val courses: List<Course>) : CourseUiState
    data class Error(val message: String) : CourseUiState
}
