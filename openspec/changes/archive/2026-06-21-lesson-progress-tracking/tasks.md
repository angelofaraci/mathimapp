# Tasks: Lesson Progress Tracking

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450–480 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Backend) → PR 2 (Client sync) |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend: shared models, server tables, service, routes, tests | PR 1 | Base = main; ~260 lines — standalone deployable |
| 2 | Client sync: SQLDelight schema, API, repository, local sync, tests | PR 2 | Base = main; ~190 lines — depends on PR 1 API being deployed |

## Phase 1: Shared & Server Foundation

- [x] 1.1 Add DTOs to `shared/Models.kt`: `CompleteExerciseRequest`, `ExerciseCompletionResponse`; extend `UserProgress` with `completedExerciseIds`
- [x] 1.2 Add `CompletedExercises(userId, exerciseId, score)` table with composite PK + cascade FKs in `server/Tables.kt`
- [x] 1.3 Register `CompletedExercises` in `server/Database.kt` SchemaUtils.create

## Phase 2: Server Completion Logic

- [x] 2.1 Add `completeExercise()` to `UserService.kt` — access validation, idempotent insert, score update, lesson derivation
- [x] 2.2 Add `POST /exercises/{id}/complete` route in `userRoutes.kt` with JWT auth, role=LEARNER, path/body match
- [x] 2.3 Block learner `POST /progress` in `userRoutes.kt` with deprecation guard (admin-only escape hatch)

## Phase 3: Client Sync

- [x] 3.1 Add local `CompletedExercise` table + insert/select queries in `AppDatabase.sq`
- [x] 3.2 Add `completeExercise()` call to `UserApi.kt`
- [x] 3.3 Add `completeExercise()` to `UserRepository.kt`; implement in `KtorUserRepository.kt` with local merge
- [x] 3.4 Register SQLDelight adapters in `AppModule.kt` if generated types require them

## Phase 4: Testing

- [x] 4.1 Server service tests: first completion, duplicate idempotency, last-exercise completes lesson, private access rejection
- [x] 4.2 Server route tests: auth identity, exercise-id mismatch, direct `/progress` learner rejection
- [x] 4.3 Client sync tests: API path/body, local completion merge, duplicate sync preserves single record
