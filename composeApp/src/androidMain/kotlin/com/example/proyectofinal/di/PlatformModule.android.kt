package com.example.proyectofinal.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.core.module.Module
import org.koin.dsl.module

@Composable
actual fun rememberPlatformModule(): Module {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        module {
            single { DatabaseDriverFactory(context) }
        }
    }
}
