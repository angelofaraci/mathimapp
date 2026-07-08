package com.example.proyectofinal.service

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.dbQuery
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.CreateAdminExerciseRequest
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExercisePayload
import com.example.proyectofinal.models.ExerciseSubmission
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.InputValueSubmission
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultiSelectSubmission
import com.example.proyectofinal.models.MultipleChoicePayload
import com.example.proyectofinal.models.MultipleChoiceSubmission
import com.example.proyectofinal.models.UpdateAdminExerciseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

sealed interface AdminExerciseMutationResult {
    data class Success(val exercise: Exercise) : AdminExerciseMutationResult
    data class InvalidRequest(val message: String) : AdminExerciseMutationResult
    object NotFound : AdminExerciseMutationResult
}

class ExerciseService {
    fun getExercisesByLessonId(lessonId: String, hideAnswers: Boolean): List<Exercise> = dbQuery {
        Exercises.selectAll()
            .where { Exercises.lessonId eq lessonId }
            .map { it.toExercise(hideAnswers) }
    }

    fun listExercisesAdmin(lessonId: String? = null): List<Exercise> = dbQuery {
        val query = Exercises.selectAll()

        val filtered = if (lessonId == null) {
            query
        } else {
            query.where { Exercises.lessonId eq lessonId }
        }

        filtered.map { it.toExercise() }
    }

    fun createExercise(request: CreateExerciseRequest): Exercise = dbQuery {
        persistCreate(request)
    }

    fun adminCreateExercise(request: CreateAdminExerciseRequest): AdminExerciseMutationResult = dbQuery {
        if (!lessonExists(request.lessonId)) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest("lessonId references an unknown lesson")
        }

        val draft = try {
            ExercisePayloadSupport.createDraft(
                id = request.id,
                lessonId = request.lessonId,
                title = request.title,
                type = request.type,
                payload = request.payload
            )
        } catch (exception: IllegalArgumentException) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest(exception.message ?: "Invalid exercise payload")
        }

        AdminExerciseMutationResult.Success(insertDraft(draft))
    }

    fun updateExercise(id: String, request: UpdateExerciseRequest): Exercise? = dbQuery {
        val existing = loadDraft(id) ?: return@dbQuery null
        val merged = mergeDraft(existing, request)
        Exercises.update({ Exercises.id eq id }) { row ->
            row[Exercises.question] = merged.title
            row[Exercises.options] = ExercisePayloadSupport.toLegacyOptionsCsv(merged.payload)
            row[Exercises.correctAnswer] = ExercisePayloadSupport.toLegacyCorrectAnswer(merged.payload)
            row[Exercises.type] = merged.type.name
            row[Exercises.payload] = ExercisePayloadSupport.serializePayload(merged.payload)
        }

        Exercises.selectAll()
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.toExercise()
    }

    fun adminUpdateExercise(id: String, request: UpdateAdminExerciseRequest): AdminExerciseMutationResult = dbQuery {
        if (request.lessonId != null && !lessonExists(request.lessonId)) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest("lessonId references an unknown lesson")
        }

        val existing = loadDraft(id) ?: return@dbQuery AdminExerciseMutationResult.NotFound

        val merged = try {
            mergeDraft(existing, request)
        } catch (exception: IllegalArgumentException) {
            return@dbQuery AdminExerciseMutationResult.InvalidRequest(exception.message ?: "Invalid exercise payload")
        }

        Exercises.update({ Exercises.id eq id }) { row ->
            row[Exercises.lessonId] = merged.lessonId
            row[Exercises.question] = merged.title
            row[Exercises.options] = ExercisePayloadSupport.toLegacyOptionsCsv(merged.payload)
            row[Exercises.correctAnswer] = ExercisePayloadSupport.toLegacyCorrectAnswer(merged.payload)
            row[Exercises.type] = merged.type.name
            row[Exercises.payload] = ExercisePayloadSupport.serializePayload(merged.payload)
        }

        val exercise = Exercises.selectAll()
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.toExercise()
            ?: return@dbQuery AdminExerciseMutationResult.NotFound

        AdminExerciseMutationResult.Success(exercise)
    }

    fun adminDeleteExercise(id: String): Boolean = deleteExercise(id)

    fun deleteExercise(id: String): Boolean = dbQuery {
        Exercises.deleteWhere { Exercises.id eq id } > 0
    }

    fun getLessonCreatorId(lessonId: String): String? = dbQuery {
        resolveLessonMutationOwnerId(lessonId)
    }

    fun getCreatorId(id: String): String? = dbQuery {
        val lessonId = Exercises.select(Exercises.lessonId)
            .where { Exercises.id eq id }
            .firstOrNull()
            ?.get(Exercises.lessonId)
            ?: return@dbQuery null

        resolveLessonMutationOwnerId(lessonId)
    }

    private fun persistCreate(request: CreateExerciseRequest): Exercise {
        val draft = ExercisePayloadSupport.createDraft(
            id = request.id,
            lessonId = request.lessonId,
            title = request.title,
            type = request.type,
            payload = request.payload
        )

        return insertDraft(draft)
    }

    private fun insertDraft(draft: PersistedExerciseDraft): Exercise {
        Exercises.insert {
            it[Exercises.id] = draft.id
            it[Exercises.lessonId] = draft.lessonId
            it[Exercises.question] = draft.title
            it[Exercises.options] = ExercisePayloadSupport.toLegacyOptionsCsv(draft.payload)
            it[Exercises.correctAnswer] = ExercisePayloadSupport.toLegacyCorrectAnswer(draft.payload)
            it[Exercises.type] = draft.type.name
            it[Exercises.payload] = ExercisePayloadSupport.serializePayload(draft.payload)
        }

        return Exercise(
            id = draft.id,
            lessonId = draft.lessonId,
            title = draft.title,
            type = draft.type,
            payload = draft.payload
        )
    }

    private fun mergeDraft(existing: PersistedExerciseDraft, request: UpdateExerciseRequest): PersistedExerciseDraft {
        val targetType = request.type ?: existing.type
        val targetPayload = when {
            request.type != null && request.payload == null -> {
                throw IllegalArgumentException("payload is required when type changes")
            }

            else -> request.payload ?: existing.payload
        }

        return ExercisePayloadSupport.createDraft(
            id = existing.id,
            lessonId = existing.lessonId,
            title = request.title ?: existing.title,
            type = targetType,
            payload = targetPayload
        )
    }

    private fun mergeDraft(existing: PersistedExerciseDraft, request: UpdateAdminExerciseRequest): PersistedExerciseDraft {
        val targetType = request.type ?: existing.type
        val targetPayload = when {
            request.type != null && request.payload == null -> {
                throw IllegalArgumentException("payload is required when type changes")
            }

            else -> request.payload ?: existing.payload
        }

        return ExercisePayloadSupport.createDraft(
            id = existing.id,
            lessonId = request.lessonId ?: existing.lessonId,
            title = request.title ?: existing.title,
            type = targetType,
            payload = targetPayload
        )
    }

    private fun loadDraft(id: String): PersistedExerciseDraft? = Exercises.selectAll()
        .where { Exercises.id eq id }
        .firstOrNull()
        ?.let { row ->
            val materialized = ExercisePayloadSupport.materializePayload(
                persistedType = row[Exercises.type],
                persistedPayload = row[Exercises.payload],
                legacyOptions = row[Exercises.options],
                legacyCorrectAnswer = row[Exercises.correctAnswer]
            )

            PersistedExerciseDraft(
                id = row[Exercises.id],
                lessonId = row[Exercises.lessonId],
                title = row[Exercises.question],
                type = materialized.type,
                payload = materialized.payload
            )
        }

    private fun lessonExists(lessonId: String): Boolean =
        Lessons.selectAll()
            .where { Lessons.id eq lessonId }
            .count() > 0

    private fun resolveLessonMutationOwnerId(lessonId: String): String? {
        val lesson = Lessons.select(Lessons.courseId, Lessons.creatorId)
            .where { Lessons.id eq lessonId }
            .firstOrNull()
            ?: return null

        val courseId = lesson[Lessons.courseId]

        return if (courseId != null) {
            Courses.select(Courses.creatorId)
                .where { Courses.id eq courseId }
                .firstOrNull()
                ?.get(Courses.creatorId)
                ?: lesson[Lessons.creatorId]
        } else {
            lesson[Lessons.creatorId]
        }
    }
}

internal data class PersistedExerciseDraft(
    val id: String,
    val lessonId: String,
    val title: String,
    val type: ExerciseType,
    val payload: ExercisePayload
)

internal data class MaterializedExercisePayload(
    val type: ExerciseType,
    val payload: ExercisePayload
)

internal data class ExerciseAttemptEvaluation(
    val isCorrect: Boolean,
    val message: String? = null
)

internal object ExercisePayloadSupport {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        explicitNulls = false
    }

    fun createDraft(
        id: String,
        lessonId: String,
        title: String,
        type: ExerciseType,
        payload: ExercisePayload
    ): PersistedExerciseDraft {
        if (title.isBlank()) {
            throw IllegalArgumentException("title is required")
        }

        val normalizedType = normalizeType(type)
        validatePayload(normalizedType, payload)

        return PersistedExerciseDraft(
            id = id,
            lessonId = lessonId,
            title = title,
            type = normalizedType,
            payload = payload
        )
    }

    fun serializePayload(payload: ExercisePayload): String =
        json.encodeToString<ExercisePayload>(payload)

    fun materializePayload(
        persistedType: String,
        persistedPayload: String,
        legacyOptions: String,
        legacyCorrectAnswer: String
    ): MaterializedExercisePayload {
        val fallback = legacyPayload(
            persistedType = persistedType,
            legacyOptions = legacyOptions,
            legacyCorrectAnswer = legacyCorrectAnswer
        )

        if (persistedPayload.isBlank()) {
            return fallback
        }

        return runCatching {
            MaterializedExercisePayload(
                type = normalizeType(persistedType),
                payload = json.decodeFromString<ExercisePayload>(persistedPayload)
            )
        }.getOrElse { fallback }
    }

    fun toExercise(
        id: String,
        lessonId: String,
        title: String,
        persistedType: String,
        persistedPayload: String,
        legacyOptions: String,
        legacyCorrectAnswer: String,
        hideAnswers: Boolean
    ): Exercise {
        val materialized = materializePayload(
            persistedType = persistedType,
            persistedPayload = persistedPayload,
            legacyOptions = legacyOptions,
            legacyCorrectAnswer = legacyCorrectAnswer
        )

        return Exercise(
            id = id,
            lessonId = lessonId,
            title = title,
            type = materialized.type,
            payload = if (hideAnswers) stripAnswers(materialized.payload) else materialized.payload
        )
    }

    fun toLegacyOptionsCsv(payload: ExercisePayload): String =
        when (payload) {
            is MultipleChoicePayload -> payload.options.joinToString(",") { it.text }
            is InputValuePayload -> ""
            is MultiSelectPayload -> payload.options.joinToString(",") { it.text }
        }

    fun toLegacyCorrectAnswer(payload: ExercisePayload): String =
        when (payload) {
            is MultipleChoicePayload -> payload.correctOptionId
                ?.let { correctId -> payload.options.firstOrNull { it.id == correctId }?.text }
                .orEmpty()

            is InputValuePayload -> payload.correctValue.orEmpty()
            is MultiSelectPayload -> payload.correctOptionIds
                ?.mapNotNull { correctId -> payload.options.firstOrNull { it.id == correctId }?.text }
                ?.joinToString(",")
                .orEmpty()
        }

    fun stripAnswers(payload: ExercisePayload): ExercisePayload =
        when (payload) {
            is MultipleChoicePayload -> payload.copy(correctOptionId = null)
            is InputValuePayload -> payload.copy(correctValue = null)
            is MultiSelectPayload -> payload.copy(correctOptionIds = null)
        }

    fun evaluateAttempt(exercise: Exercise, submission: ExerciseSubmission): ExerciseAttemptEvaluation =
        when (val payload = exercise.payload) {
            is MultipleChoicePayload -> {
                val answer = submission as? MultipleChoiceSubmission
                    ?: throw IllegalArgumentException("submission type does not match exercise type")
                val selectedOptionId = answer.selectedOptionId.trim()
                if (selectedOptionId.isBlank()) {
                    throw IllegalArgumentException("selectedOptionId is required")
                }
                if (payload.options.none { it.id == selectedOptionId }) {
                    throw IllegalArgumentException("selectedOptionId references an unknown option")
                }

                ExerciseAttemptEvaluation(
                    isCorrect = selectedOptionId == payload.correctOptionId,
                    message = if (selectedOptionId == payload.correctOptionId) null else "Incorrect answer. Try again."
                )
            }

            is InputValuePayload -> {
                val answer = submission as? InputValueSubmission
                    ?: throw IllegalArgumentException("submission type does not match exercise type")
                val normalizedValue = answer.value.trim()
                if (normalizedValue.isBlank()) {
                    throw IllegalArgumentException("value is required")
                }

                ExerciseAttemptEvaluation(
                    isCorrect = normalizedValue.equals(payload.correctValue?.trim(), ignoreCase = true),
                    message = if (normalizedValue.equals(payload.correctValue?.trim(), ignoreCase = true)) {
                        null
                    } else {
                        "Incorrect answer. Try again."
                    }
                )
            }

            is MultiSelectPayload -> {
                val answer = submission as? MultiSelectSubmission
                    ?: throw IllegalArgumentException("submission type does not match exercise type")
                val selectedIds = answer.selectedOptionIds
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                if (selectedIds.isEmpty()) {
                    throw IllegalArgumentException("selectedOptionIds must contain at least one option")
                }
                if (selectedIds.any { selectedId -> payload.options.none { it.id == selectedId } }) {
                    throw IllegalArgumentException("selectedOptionIds reference an unknown option")
                }

                ExerciseAttemptEvaluation(
                    isCorrect = selectedIds.toSet() == payload.correctOptionIds.orEmpty().toSet(),
                    message = if (selectedIds.toSet() == payload.correctOptionIds.orEmpty().toSet()) {
                        null
                    } else {
                        "Incorrect answer. Try again."
                    }
                )
            }
        }

    fun legacyPayloadJson(
        type: ExerciseType = ExerciseType.MULTIPLE_CHOICE,
        optionsCsv: String,
        correctAnswer: String
    ): String = serializePayload(
        legacyPayload(
            persistedType = type.name,
            legacyOptions = optionsCsv,
            legacyCorrectAnswer = correctAnswer
        ).payload
    )

    private fun validatePayload(type: ExerciseType, payload: ExercisePayload) {
        when (type) {
            ExerciseType.MULTIPLE_CHOICE -> {
                val choicePayload = payload as? MultipleChoicePayload
                    ?: throw IllegalArgumentException("payload must be a MultipleChoicePayload for MULTIPLE_CHOICE")
                validateChoiceOptions(choicePayload.options)
                if (choicePayload.options.size < 2) {
                    throw IllegalArgumentException("MultipleChoice exercises require at least 2 options")
                }
                val correctOptionId = choicePayload.correctOptionId?.trim().orEmpty()
                if (correctOptionId.isBlank()) {
                    throw IllegalArgumentException("correctOptionId is required")
                }
                if (choicePayload.options.none { it.id == correctOptionId }) {
                    throw IllegalArgumentException("correctOptionId must reference a valid option")
                }
            }

            ExerciseType.INPUT_VALUE -> {
                val inputPayload = payload as? InputValuePayload
                    ?: throw IllegalArgumentException("payload must be an InputValuePayload for INPUT_VALUE")
                if (inputPayload.correctValue?.trim().isNullOrEmpty()) {
                    throw IllegalArgumentException("correctValue is required")
                }
            }

            ExerciseType.MULTI_SELECT -> {
                val multiSelectPayload = payload as? MultiSelectPayload
                    ?: throw IllegalArgumentException("payload must be a MultiSelectPayload for MULTI_SELECT")
                validateChoiceOptions(multiSelectPayload.options)
                if (multiSelectPayload.options.size < 2) {
                    throw IllegalArgumentException("MultiSelect exercises require at least 2 options")
                }
                val correctOptionIds = multiSelectPayload.correctOptionIds.orEmpty()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                if (correctOptionIds.isEmpty()) {
                    throw IllegalArgumentException("correctOptionIds must contain at least 1 option")
                }
                if (correctOptionIds.any { correctId -> multiSelectPayload.options.none { it.id == correctId } }) {
                    throw IllegalArgumentException("correctOptionIds must reference valid options")
                }
            }

            ExerciseType.TRUE_FALSE -> error("TRUE_FALSE must be normalized before validation")
        }
    }

    private fun validateChoiceOptions(options: List<ChoiceOption>) {
        if (options.any { it.id.isBlank() || it.text.isBlank() }) {
            throw IllegalArgumentException("options must contain non-blank id and text values")
        }
        if (options.map(ChoiceOption::id).distinct().size != options.size) {
            throw IllegalArgumentException("option ids must be unique")
        }
    }

    private fun legacyPayload(
        persistedType: String,
        legacyOptions: String,
        legacyCorrectAnswer: String
    ): MaterializedExercisePayload {
        val normalizedType = normalizeType(persistedType)
        val optionTexts = legacyOptions.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)

        val payload = when (normalizedType) {
            ExerciseType.MULTIPLE_CHOICE -> {
                val options = optionTexts.map { option -> ChoiceOption(id = option, text = option) }
                    .let { mappedOptions ->
                        if (legacyCorrectAnswer.trim().isNotEmpty() && mappedOptions.none { it.id == legacyCorrectAnswer.trim() }) {
                            mappedOptions + ChoiceOption(id = legacyCorrectAnswer.trim(), text = legacyCorrectAnswer.trim())
                        } else {
                            mappedOptions
                        }
                    }
                MultipleChoicePayload(
                    options = options,
                    correctOptionId = options.firstOrNull { it.text == legacyCorrectAnswer.trim() }?.id
                )
            }

            ExerciseType.INPUT_VALUE -> InputValuePayload(correctValue = legacyCorrectAnswer)
            ExerciseType.MULTI_SELECT -> {
                val options = optionTexts.map { option -> ChoiceOption(id = option, text = option) }
                val selectedIds = legacyCorrectAnswer.split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                MultiSelectPayload(
                    options = options,
                    correctOptionIds = selectedIds.ifEmpty { null }
                )
            }

            ExerciseType.TRUE_FALSE -> error("TRUE_FALSE must be normalized before payload fallback")
        }

        return MaterializedExercisePayload(
            type = normalizedType,
            payload = payload
        )
    }

    private fun normalizeType(type: ExerciseType): ExerciseType =
        if (type == ExerciseType.TRUE_FALSE) {
            ExerciseType.MULTIPLE_CHOICE
        } else {
            type
        }

    private fun normalizeType(persistedType: String): ExerciseType {
        val rawType = ExerciseType.valueOf(persistedType.uppercase())
        return normalizeType(rawType)
    }
}
