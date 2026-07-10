package com.example.proyectofinal.di

import android.os.Build
import com.example.proyectofinal.BuildConfig

internal actual fun getApiBaseUrl(): String =
    if (isRunningOnEmulator()) {
        BuildConfig.EMULATOR_API_BASE_URL
    } else {
        BuildConfig.PHYSICAL_DEVICE_API_BASE_URL
    }

private fun isRunningOnEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.orEmpty()
    val model = Build.MODEL.orEmpty()
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val brand = Build.BRAND.orEmpty()
    val device = Build.DEVICE.orEmpty()
    val product = Build.PRODUCT.orEmpty()
    val hardware = Build.HARDWARE.orEmpty()

    return fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        model.contains("google_sdk") ||
        model.contains("Emulator") ||
        model.contains("Android SDK built for x86") ||
        manufacturer.contains("Genymotion") ||
        (brand.startsWith("generic") && device.startsWith("generic")) ||
        product == "google_sdk" ||
        product.contains("sdk_gphone") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu")
}
