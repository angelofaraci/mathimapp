package com.example.proyectofinal

import androidx.compose.ui.window.ComposeUIViewController
import com.example.proyectofinal.di.DatabaseDriverFactory
import com.example.proyectofinal.di.initializeKoin
import org.koin.dsl.module

fun MainViewController() = run {
    initializeKoin(
        module {
            single { DatabaseDriverFactory() }
        }
    )

    ComposeUIViewController { App() }
}
