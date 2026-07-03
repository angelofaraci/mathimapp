package com.example.proyectofinal.ui.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class ActivitiesTabRouterTest {

    @Test
    fun `router defaults to catalog`() {
        val router = ActivitiesTabRouter()

        assertEquals(ActivitiesTabRoute.Catalog, router.target.value)
    }

    @Test
    fun `router transitions to detail and back to catalog`() {
        val router = ActivitiesTabRouter()

        router.showDetail("course-42")
        assertEquals(ActivitiesTabRoute.Detail("course-42"), router.target.value)

        router.showCatalog()
        assertEquals(ActivitiesTabRoute.Catalog, router.target.value)
    }
}
