package com.example.proyectofinal.ui.home

import java.time.LocalTime

internal actual fun currentLocalHour(): Int = LocalTime.now().hour
