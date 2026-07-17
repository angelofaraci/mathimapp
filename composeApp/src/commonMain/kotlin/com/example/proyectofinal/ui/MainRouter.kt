package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MainTab { HOME, ACTIVITIES, PROGRESS, PROFILE }

class MainRouterViewModel(initialTab: MainTab = MainTab.HOME) : ViewModel() {
    private val _target = MutableStateFlow(initialTab)

    val target: StateFlow<MainTab> = _target.asStateFlow()

    fun select(tab: MainTab) {
        _target.value = tab
    }

    fun showHome() {
        select(MainTab.HOME)
    }

    fun showActivities() {
        select(MainTab.ACTIVITIES)
    }

    fun showProgress() {
        select(MainTab.PROGRESS)
    }

    fun showProfile() {
        select(MainTab.PROFILE)
    }
}
