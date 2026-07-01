package com.example.proyectofinal.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.models.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val defaultCatalogTopics = listOf("Fracciones", "Álgebra", "Geometría")

class CourseCatalogViewModel(
    private val courseRepository: CourseRepository,
    private val learnerProfileRepository: LearnerProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseCatalogUiState())
    val uiState: StateFlow<CourseCatalogUiState> = _uiState.asStateFlow()

    init {
        fetchCourses()
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleTopic(topic: String) {
        _uiState.update { state ->
            state.copy(selectedTopic = if (state.selectedTopic == topic) null else topic)
        }
    }

    fun retry() {
        fetchCourses()
    }

    private fun fetchCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(remoteState = CourseCatalogRemoteState.Loading) }

            try {
                val schoolYear = learnerProfileRepository.getProfile()?.schoolYear
                val courses = courseRepository.getOfficialCourses(schoolYear)
                _uiState.update { it.copy(remoteState = CourseCatalogRemoteState.Success(courses)) }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        remoteState = CourseCatalogRemoteState.Error(
                            exception.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }
}

data class CourseCatalogUiState(
    val remoteState: CourseCatalogRemoteState = CourseCatalogRemoteState.Loading,
    val query: String = "",
    val selectedTopic: String? = null,
    val topics: List<String> = defaultCatalogTopics
) {
    val visibleCourses: List<Course>
        get() = (remoteState as? CourseCatalogRemoteState.Success)
            ?.courses
            ?.filter(::matchesFilters)
            .orEmpty()

    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || selectedTopic != null

    private fun matchesFilters(course: Course): Boolean {
        val matchesQuery = query.isBlank() || course.title.contains(query.trim(), ignoreCase = true)
        val matchesTopic = selectedTopic == null || course.topic?.equals(selectedTopic, ignoreCase = true) == true
        return matchesQuery && matchesTopic
    }
}

sealed interface CourseCatalogRemoteState {
    data object Loading : CourseCatalogRemoteState
    data class Success(val courses: List<Course>) : CourseCatalogRemoteState
    data class Error(val message: String) : CourseCatalogRemoteState
}
