# Design: Typed Exercise Payloads (Phase 1)

## Technical Approach

Adopt a shared typed `Exercise` contract, persist payloads as JSON, validate answers on the backend, and render/administer each type through type-dispatched UI. This follows the proposal and avoids leaking correct answers to students while still supporting immediate retry.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|---|---|---|---|
| Shared exercise shape | Keep flat fields; add payload beside flat fields; replace shared contract with typed payload | Replace the shared contract with `Exercise(id, lessonId, title, type, payload)` | Matches the shipped contract, removes string parsing, and keeps only persisted base metadata alongside extensibility in `payload`. |
| Student vs admin payloads | Separate DTO hierarchies; same payload with nullable answers | Same payload model, but answer fields are nullable and stripped to `null` for student reads | One contract across app/server/admin. With `Json { classDiscriminator = "type"; explicitNulls = false }`, student responses omit answer fields entirely. |
| Answer validation | Client-side using correct answers; backend attempt validation | Backend attempt validation | Student payloads MUST hide answers. Client-side validation would violate that requirement. |
| Migration strategy | Dual format; destructive cutover; backfilled JSON | Backfill `payload` JSON, keep old flat DB columns temporarily, coordinated deploy | Existing rows stay readable, rollback is non-destructive, and code only needs one live format. |

## Data Flow

### Admin save

Admin Web editor -> `POST/PUT /admin/exercises` -> `ExerciseService.validatePayload()` -> Exposed row (`question`, `type`, `payload`) -> admin response with full payload

### Student play

Lesson read -> `LessonService` -> `stripAnswersForStudent()` -> Compose player by payload type -> `POST /exercises/{id}/attempt` -> backend validates against stored full payload -> wrong: feedback only / correct: persist completion and advance

## File Changes

| File | Action | Description |
|---|---|---|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Replace flat `Exercise`; add `ExercisePayload`, `ChoiceOption`, `ExerciseAttemptRequest/Response`, and typed submissions. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Add `payload` column; widen `type`; keep old flat columns temporarily. |
| `server/src/main/resources/db/migration/V5__exercise_payload_json.sql` | Create | Add/backfill `payload`, map `TRUE_FALSE` rows to `MULTIPLE_CHOICE`, then make `payload` non-null. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ExerciseService.kt` | Modify | Centralize payload serialization, deserialization, and per-type validation. |
| `server/src/main/kotlin/com/example/proyectofinal/service/ServiceMappers.kt` | Modify | Decode `payload`; map persisted `question` column into shared `title`; strip answers for students. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/adminRoutes.kt` | Modify | Admin CRUD consumes/returns typed payloads. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` | Modify | Add attempt endpoint for student answer submission. |
| `server/src/main/kotlin/com/example/proyectofinal/service/UserService.kt` | Modify | Validate attempts before completion is recorded. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapViewModel.kt` | Modify | Replace `selectedAnswer: String?` with typed draft/attempt flow and retry state. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapScreen.kt` | Modify | Dispatch to type-specific player composables and feedback states. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/UserApi.kt` | Modify | Send typed attempt requests. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Store `payload` JSON and `title`; stop caching flat answer fields. |
| `admin-web/src/pages/Exercises.tsx` | Modify | Convert form state to a discriminated union and submit typed payloads. |

## Interfaces / Contracts

```kotlin
@Serializable
data class Exercise(
    val id: String,
    val lessonId: String,
    val title: String,
    val type: ExerciseType,
    val payload: ExercisePayload
)

@Serializable enum class ExerciseType { MULTIPLE_CHOICE, INPUT_VALUE, MULTI_SELECT }
@Serializable data class ChoiceOption(val id: String, val text: String)
@Serializable sealed interface ExercisePayload
@Serializable @SerialName("multipleChoice") data class MultipleChoicePayload(val options: List<ChoiceOption>, val correctOptionId: String? = null) : ExercisePayload
@Serializable @SerialName("inputValue") data class InputValuePayload(val placeholder: String? = null, val correctValue: String? = null) : ExercisePayload
@Serializable @SerialName("multiSelect") data class MultiSelectPayload(val options: List<ChoiceOption>, val correctOptionIds: List<String>? = null) : ExercisePayload
```

Student lesson reads return the same payloads with answer fields omitted. Admin create/update requires those fields to be present and valid.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Payload serializer, student answer stripping, validator rules, attempt evaluation | `server:test` and `composeApp:jvmTest` table-driven tests |
| Integration | Admin CRUD, migration backfill, attempt endpoint wrong/correct flows, lesson reads hide answers | Extend `AdminIntegrationTest`, `ServerIntegrationTest`, `ServiceLayerTest` |
| UI | ViewModel retry/advance logic, repository payload caching | Extend `LessonMapViewModelTest`, `KtorExerciseRepositoryTest` |
| Admin web | Per-type editor behavior | Manual checklist in Phase 1; no runner exists yet |

## Migration / Rollout

1. Flyway V5 adds nullable `payload` TEXT.
2. Backfill JSON from current rows: `MULTIPLE_CHOICE` and `TRUE_FALSE` -> `MultipleChoicePayload`; `INPUT_VALUE` -> `InputValuePayload`.
3. Normalize persisted `TRUE_FALSE` type to `MULTIPLE_CHOICE`.
4. Set `payload` to `NOT NULL` and deploy backend/app/admin together.
5. Keep `question/options/correct_answer` columns for rollback; drop them in a later cleanup change.

## Open Questions

- [ ] None blocking.
