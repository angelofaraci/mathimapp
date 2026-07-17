package com.example.proyectofinal.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MainRouterViewModelTest {

    @Test
    fun `default target is home`() {
        val router = MainRouterViewModel()

        assertEquals(MainTab.HOME, router.target.value)
    }

    @Test
    fun `select updates the active tab`() {
        val router = MainRouterViewModel()

        router.select(MainTab.PROFILE)

        assertEquals(MainTab.PROFILE, router.target.value)
    }

    @Test
    fun `helper methods switch between tabs`() {
        val router = MainRouterViewModel()

        router.showActivities()
        assertEquals(MainTab.ACTIVITIES, router.target.value)

        router.showProgress()
        assertEquals(MainTab.PROGRESS, router.target.value)

        router.showProfile()
        assertEquals(MainTab.PROFILE, router.target.value)

        router.showHome()
        assertEquals(MainTab.HOME, router.target.value)
    }

    @Test
    fun `rapid tab switching keeps the last selected tab`() {
        val router = MainRouterViewModel()

        router.showActivities()
        router.showProfile()
        router.showProgress()

        assertEquals(MainTab.PROGRESS, router.target.value)
    }

    @Test
    fun `selected tab remains when the view model is reused`() {
        val viewModel = MainRouterViewModel()

        viewModel.showProfile()
        val reusedViewModel = viewModel

        assertEquals(MainTab.PROFILE, reusedViewModel.target.value)
    }

    @Test
    fun `logout resets the retained tab before a subsequent authenticated session`() {
        val viewModel = MainRouterViewModel().apply { showProfile() }
        var tabWhenLogoutRuns: MainTab? = null

        logoutFromAuthenticatedHome(viewModel) {
            tabWhenLogoutRuns = viewModel.target.value
        }

        assertEquals(MainTab.HOME, tabWhenLogoutRuns)
        assertEquals(MainTab.HOME, viewModel.target.value)
    }
}
