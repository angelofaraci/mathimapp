package com.example.proyectofinal.ui.catalog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ActivitiesTabRoute {
    data object Catalog : ActivitiesTabRoute

    data class Detail(val courseId: String) : ActivitiesTabRoute
}

class ActivitiesTabRouter(initialRoute: ActivitiesTabRoute = ActivitiesTabRoute.Catalog) {
    private val _target = MutableStateFlow(initialRoute)

    val target: StateFlow<ActivitiesTabRoute> = _target.asStateFlow()

    fun showCatalog() {
        _target.value = ActivitiesTabRoute.Catalog
    }

    fun showDetail(courseId: String) {
        _target.value = ActivitiesTabRoute.Detail(courseId)
    }
}
