# Proposal: Lesson Progress Tracking

## Intent

Replace coarse lesson-level progress with granular exercise completion tracking. A lesson is completed only when all its exercises are done. Progress must be cumulative, idempotent, and enforced server-side.

## Scope

### In Scope
- Server `CompletedExercises` table and authenticated exercise-completion endpoint.
- Server-side derivation of lesson completion from exercise completions.
- Cumulative, idempotent scoring per student.
- SQLDelight schema update and client-side sync.
- `UserRepository` interface updates for progress methods.

### Out of Scope
- Progressive hints, photo submissions, teacher exercise creator.
- Full gamification (badges, achievements).
- Complete lesson/exercise UI; only contracts/sync needed.

## Capabilities

> This section is the CONTRACT between proposal and specs phases.

### New Capabilities
- `exercise-completion`: Submit and track individual exercise completions per student.
- `lesson-progress-derivation`: Derive lesson completion automatically when all exercises are finished.
- `progress-sync`: Sync granular progress between server and client.

### Modified Capabilities
- None (no existing OpenSpec capabilities).

## Approach

Adopt Server-Driven Auto-Completion. The client POSTs `CompleteExerciseRequest(exerciseId, score)` to a new authenticated endpoint. The server inserts the completion (ignoring duplicates), adds the score to the user's total, counts completed exercises for the lesson, and auto-marks the lesson complete when the threshold is reached.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `shared/src/.../Models.kt` | Modified | New request/response DTOs. |
| `server/src/.../Tables.kt` | New | `CompletedExercises` table. |
| `server/src/.../UserService.kt` | Modified | Completion logic, score sum, lesson derivation. |
| `server/src/.../userRoutes.kt` | Modified | New `POST /exercises/{id}/complete` route. |
| `server/src/test/...` | Modified | Tests for completion flow. |
| `composeApp/src/.../AppDatabase.sq` | New | `CompletedExercise` table/queries. |
| `composeApp/src/.../UserApi.kt` | Modified | New client API call. |
| `composeApp/src/.../KtorUserRepository.kt` | Modified | Sync local completions. |
| `composeApp/src/.../UserRepository.kt` | Modified | Add progress methods. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Old `POST /progress` bypasses new rules | Med | Deprecate for students; restrict to admins. |
| SQLDelight schema migration needed | Low | Bump version; add migration if app is distributed. |
| No exercise UI to verify end-to-end | Med | Build backend first; UI in separate scope. |

## Rollback Plan

1. Remove `CompletedExercises` table and endpoint.
2. Revert `UserService` to direct lesson-marking.
3. Restore old SQLDelight schema version.

## Dependencies

- None.

## Success Criteria

- [ ] Student can submit exercise completion (idempotent).
- [ ] Lesson completion derives from all exercises done.
- [ ] Total score accumulates correctly.
- [ ] Progress persists across difficulty changes.
- [ ] Backend tests cover completion, idempotency, and derivation.
