package com.example.proyectofinal.data

import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.ExerciseType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object ExercisePayloadJson {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    fun encode(payload: ExercisePayload): String = json.encodeToString(payload)

    fun decode(payloadJson: String): ExercisePayload = json.decodeFromString(payloadJson)

    fun encode(exercise: Exercise): String = encode(exercise.payload)

    fun legacyPayloadJson(
        typeName: String,
        optionsCsv: String,
        correctAnswer: String,
    ): String = legacyPayloadJson(
        type = parseType(typeName),
        optionsCsv = optionsCsv,
        correctAnswer = correctAnswer,
    )

    fun legacyPayloadJson(
        type: ExerciseType,
        optionsCsv: String,
        correctAnswer: String,
    ): String {
        val legacyExercise = Exercise(
            id = "local-cache",
            lessonId = "local-cache",
            question = "",
            options = optionsCsv
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty),
            correctAnswer = correctAnswer,
            type = normalizeType(type),
        )

        return encode(legacyExercise.payload)
    }

    fun parseType(typeName: String): ExerciseType = normalizeType(
        ExerciseType.entries.firstOrNull { it.name == typeName } ?: ExerciseType.MULTIPLE_CHOICE,
    )

    private fun normalizeType(type: ExerciseType): ExerciseType =
        if (type == ExerciseType.TRUE_FALSE) {
            ExerciseType.MULTIPLE_CHOICE
        } else {
            type
        }
}
