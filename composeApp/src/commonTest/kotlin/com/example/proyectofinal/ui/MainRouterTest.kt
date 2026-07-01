package com.example.proyectofinal.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MainRouterTest {

    @Test
    fun `default target is home`() {
        val router = MainRouter()

        assertEquals(MainTab.HOME, router.target.value)
    }

    @Test
    fun `select updates the active tab`() {
        val router = MainRouter()

        router.select(MainTab.PROFILE)

        assertEquals(MainTab.PROFILE, router.target.value)
    }

    @Test
    fun `helper methods switch between tabs`() {
        val router = MainRouter()

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
        val router = MainRouter()

        router.showActivities()
        router.showProfile()
        router.showProgress()

        assertEquals(MainTab.PROGRESS, router.target.value)
    }
}
