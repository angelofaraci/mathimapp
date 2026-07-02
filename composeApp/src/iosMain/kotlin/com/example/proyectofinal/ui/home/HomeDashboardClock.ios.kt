package com.example.proyectofinal.ui.home

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSDate

internal actual fun currentLocalHour(): Int =
    NSCalendar.currentCalendar.components(NSCalendarUnitHour, fromDate = NSDate()).hour.toInt()
