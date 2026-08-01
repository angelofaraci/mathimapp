package com.example.proyectofinal.di

import com.example.proyectofinal.ui.MainRouterViewModel
import com.example.proyectofinal.ui.MainTab
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class AppModuleTest {

    @Test
    fun `MainRouterViewModel resolves from appModule without a bound MainTab`() {
        val koinApp = koinApplication {
            modules(appModule)
        }
        val koin = koinApp.koin

        val router = koin.get<MainRouterViewModel>()

        assertEquals(MainTab.HOME, router.target.value)

        koin.close()
    }
}
