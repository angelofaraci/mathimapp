package com.example.proyectofinal.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatform

fun initializeKoin(platformModule: Module) {
    if (KoinPlatform.getKoinOrNull() == null) {
        startKoin {
            modules(platformModule, appModule)
        }
    }
}
