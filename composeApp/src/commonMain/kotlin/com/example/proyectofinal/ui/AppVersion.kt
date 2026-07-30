package com.example.proyectofinal.ui

/**
 * Resolves the human-readable app version for profile surfaces. Android reads the
 * Gradle-injected [com.example.proyectofinal.BuildConfig.VERSION_NAME]; other targets
 * pin the same declared version until a shared build-time source exists.
 */
internal expect fun appVersionName(): String
