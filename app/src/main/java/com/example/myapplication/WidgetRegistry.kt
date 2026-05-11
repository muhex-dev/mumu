package com.example.myapplication

/**
 * Central registry of all available top-section widgets.
 * Add new widgets here and nowhere else.
 */
sealed class TopWidget(
    val id: String,
    val label: String,
    val category: String,
    val previewDrawableName: String   // matches a drawable filename, e.g. "prev_rotating_clock"
) {
    object RotatingClock : TopWidget("rotating_clock", "Rotating Clock", "Clock", "prev_rotating_clock")
    object ClockDate     : TopWidget("clock_date",     "Clock & Date",   "Clock", "prev_clock_date")
    object ModernStack   : TopWidget("modern_stack",   "Modern Stack",   "Clock", "prev_modern_stack")
    object InfoClock     : TopWidget("info_clock",     "Info Clock",     "Info",  "prev_info_clock")
    object Greeting      : TopWidget("greeting",       "Greeting",       "Info",  "prev_greeting")
    object BigNumeric    : TopWidget("big_numeric",    "Big Numeric",    "Clock", "prev_big_numeric")
    object HeyToday      : TopWidget("hey_today",      "Hey Today",      "Info",  "prev_hey_today")

    companion object {
        val all: List<TopWidget> = listOf(
            RotatingClock, ClockDate, ModernStack,
            InfoClock, Greeting, BigNumeric, HeyToday
        )

        fun fromId(id: String): TopWidget? = all.find { it.id == id }

        // Default slot if prefs are empty
        val default: List<TopWidget> = listOf(RotatingClock)
    }
}
