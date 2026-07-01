package com.example.proyectofinal.di

import android.os.Build
import com.example.proyectofinal.BuildConfig

internal actual fun getApiBaseUrl(): String =
    if (isRunningOnEmulator()) {
        if (BuildConfig.PHYSICAL_DEVICE_API_BASE_URL != BuildConfig.EMULATOR_API_BASE_URL) {
            BuildConfig.PHYSICAL_DEVICE_API_BASE_URL
        } else {
            BuildConfig.EMULATOR_API_BASE_URL
        }
    } else {
        BuildConfig.PHYSICAL_DEVICE_API_BASE_URL
    }

private fun isRunningOnEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT
    val model = Build.MODEL
    val manufacturer = Build.MANUFACTURER
    val brand = Build.BRAND
    val device = Build.DEVICE
    val product = Build.PRODUCT
    val hardware = Build.HARDWARE

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
