# Design: Lesson Progress Tracking

## Technical Approach

Implement server-driven exercise completion as the progress source of truth. The client submits an authenticated exercise completion, the server derives the student from JWT, stores a first-wins completion, increments cumulative score once, and marks the parent lesson complete only after every exercise in that lesson is completed. `GET /progress/{userId}` remains the read API but returns both completed lessons and completed exercises.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|---|---|---|---|
| Completion authority | Client-derived lesson completion vs server-derived completion | Server derives lessons from `CompletedExercises` | Lesson completion is a domain invariant and must not be bypassed by clients or multiple devices. |
| Identity source | Request `userId` vs JWT user | JWT `currentUserId()` only | Prevents one student from completing exercises for another student. |
| Duplicate handling | Update score on retry vs ignore duplicates | Composite key `(userId, exerciseId)` with stored first `score` | Preserves idempotency and first-wins scoring. |
| Public/private access | Reuse lesson visibility vs selected-level gate | Reuse course visibility, restricted to `LEARNER` for completion | Public/app-curated content should not be blocked by selected school-year; future level selection will guide recommendations/paths, not authorization. Private course content still requires enrollment. |
| Direct lesson endpoint | Keep learner `POST /progress` vs deprecate | Block learners; leave admin-only/manual path if needed | Prevents bypassing exercise-derived completion while preserving migration/repair escape hatch. |

## Data Flow

```text
Client ──POST /exercises/{id}/complete──> userRoutes/exercise route
  JWT userId, role                         │
                                           ▼
                                  UserService.completeExercise
                                           │
                Exercises ─ Lessons ─ Courses access check
                                           │
                    INSERT CompletedExercises first-wins
                                           │
                 +score once ── count lesson exercises
                                           │
                       INSERT CompletedLessons when all done
                                           ▼
                              ExerciseCompletionResponse
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` | Modify | Add `completedExerciseIds` to `UserProgress`; add `CompleteExerciseRequest` and `ExerciseCompletionResponse`. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` | Modify | Add `CompletedExercises(userId, exerciseId, score)` with composite PK and cascade FKs. |
| `server/src/main/kotlin/com/example/proyectofinal/database/Database.kt` | Modify | Include `CompletedExercises` in `SchemaUtils.create`. |
| `server/src/main/kotlin/com/example/proyectofinal/service/UserService.kt` | Modify | Add completion result model, idempotent insert, score update, access validation, and derived lesson completion. |
| `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` | Modify | Add `POST /exercises/{id}/complete`; reject learner `POST /progress` direct completion. |
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Add local `CompletedExercise` table and insert/select queries. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/UserApi.kt` | Modify | Add `completeExercise()` and keep `fetchUserProgress()` sync. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/UserRepository.kt` | Modify | Expose `getUserProgress()` and `completeExercise()`. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorUserRepository.kt` | Modify | Store progress, completed lessons, and completed exercises locally after fetch/complete. |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` | Modify | Add adapters only if SQLDelight generated types require them. |
| `server/src/test/kotlin/com/example/proyectofinal/*.kt` | Modify | Add service and route coverage. |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/data/KtorUserRepositoryTest.kt` | Modify | Add API path/body and local sync tests. |

## Interfaces / Contracts

```kotlin
@Serializable
data class UserProgress(
    val userId: String,
    val completedLessonIds: Set<String> = emptySet(),
    val completedExerciseIds: Set<String> = emptySet(),
    val totalScore: Int = 0,
    val enrolledCourseIds: Set<String> = emptySet()
)

@Serializable
data class CompleteExerciseRequest(val exerciseId: String, val score: Int = 0)

@Serializable
data class ExerciseCompletionResponse(
    val exerciseId: String,
    val lessonId: String,
    val lessonCompleted: Boolean,
    val progress: UserProgress
)
```

`POST /exercises/{id}/complete` MUST require JWT auth, `UserRole.LEARNER`, matching path/body exercise ID, and no `userId` field. Public/app-curated exercises use existing public/official course visibility; private course exercises require enrollment.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit/service | First completion, duplicate ignored, final exercise completes lesson, private access rejected | Extend `ServiceLayerTest` with H2 fixtures. |
| Integration | Auth identity, forbidden foreign/direct progress, endpoint response | Extend `ServerIntegrationTest` using JWT test client. |
| App sync | API path/body, local completed exercise merge, duplicate sync | Extend `KtorUserRepositoryTest` and SQLDelight test DB. |
| E2E | Full UI exercise flow | Not in scope; no exercise UI exists yet. |

## Migration / Rollout

Add server table through `SchemaUtils.create`; existing lesson completions remain readable. Add SQLDelight local table/queries; no migration files are configured today, so distributed clients would need a schema migration before release. Deprecate learner `POST /progress` after the new endpoint and client sync are available.

## Open Questions

- None.
