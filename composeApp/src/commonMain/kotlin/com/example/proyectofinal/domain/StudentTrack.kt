package com.example.proyectofinal.domain

enum class StudentTrack(val displayName: String) {
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    TECHNICAL_SECONDARY("Technical Secondary"),
    SELF_DIRECTED("Self-directed");

    companion object {
        fun parse(value: String): StudentTrack? {
            val normalizedValue = value.normalizeStudentTrack()
            return entries.firstOrNull { track ->
                track.name.normalizeStudentTrack() == normalizedValue ||
                    track.displayName.normalizeStudentTrack() == normalizedValue
            }
        }
    }
}

private fun String.normalizeStudentTrack(): String =
    trim()
        .uppercase()
        .replace('-', '_')
        .replace(' ', '_')
