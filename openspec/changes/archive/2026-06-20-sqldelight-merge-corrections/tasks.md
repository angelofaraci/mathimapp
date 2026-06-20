# Tasks: SQLDelight Merge Security & Contract Corrections

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~310-340 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr-default |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Shared Contract & DB Schema

- [x] 1.1 Add `@Serializable data class CompleteLessonRequest(userId, lessonId, score)` to `shared/.../models/Models.kt`
- [x] 1.2 Remove server-local `CompleteLessonRequest` from `server/.../models/ProgressDto.kt`
- [x] 1.3 Convert relation columns to `reference(...)` with `onDelete = CASCADE` in `server/.../database/Tables.kt` — course→lessons/exercises/enrollments/progress; lesson→exercises/progress; student→progress/enrollments; leave teacher→courses non-cascading

## Phase 2: Server Security

- [x] 2.1 Update `server/.../plugins/Security.kt` — read JWT secret from env/config; add `currentUserId()`, `currentRole()`, `requireSelfOrAdmin()`, `requireAdmin()` helpers
- [x] 2.2 Update `server/.../routes/authRoutes.kt` — reject public ADMIN registration; allow only TEACHER/LEARNER
- [x] 2.3 Update `server/.../routes/userRoutes.kt` — enforce self/admin checks on user/progress routes; accept shared `CompleteLessonRequest`
- [x] 2.4 Update `server/.../routes/courseRoutes.kt` — require auth on all course paths (creator/enrolled/join/create/read)
- [x] 2.5 Update `server/.../routes/lessonRoutes.kt` — hide `correctAnswer` from learner lesson detail payloads
- [x] 2.6 Update `server/.../routes/exerciseRoutes.kt` — hide `correctAnswer` from learner exercise list/detail responses
- [x] 2.7 Update `server/.../seed/SeedData.kt` — load admin credentials from env/config; BCrypt hash; remove secret/credential logging

## Phase 3: Client Wiring

- [x] 3.1 Add `object TokenHolder` + `defaultRequest` `Authorization: Bearer` header injection in `composeApp/.../NetworkClient.kt`
- [x] 3.2 Update `composeApp/.../data/UserApi.kt` to send shared `CompleteLessonRequest` instead of full `UserProgress`

## Phase 4: Testing

- [x] 4.1 Extend `server/.../ServerIntegrationTest.kt` — assert 401/403 rejection, admin registration rejection, answer hiding on learner endpoints, and FK cascade integrity for course/lesson/student deletions; verify teacher-owned courses survive teacher deletion

## Remediation Notes

- 2026-06-19: Aligned change artifacts with the existing `LEARNER` role name and added Compose runtime tests for `Authorization` header injection plus memory-only token behavior.
- 2026-06-19: Added one server seed-path integration test that boots `module(..., seedData = true)` with `seed.admin.*` system properties, verifies the admin seed persists as BCrypt, and asserts captured startup output omits the configured admin password and JWT secret.
