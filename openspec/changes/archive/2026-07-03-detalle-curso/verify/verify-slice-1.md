# Verify Report: detalle-curso (Slice 1 — Shared Contract & Server Backend)

> Archived slice-1 verification content. Slice-2 is in `verify-slice-2.md`.
> The canonical `verify-report.md` mirrors the latest slice (slice 2).

## Change
- **change_name**: `detalle-curso`
- **persistence mode**: hybrid (openspec file + engram)
- **verification scope**: Slice 1 only (shared contract + server backend)
- **strict TDD**: inactive

## Completeness Table

| Slice-1 Task | Status | Evidence |
|---|---|---|
| 1.1 Add `exerciseCount: Int = 0` to `Lesson` in shared | DONE | `shared/.../Models.kt` field added with default `0`; serializable via `@Serializable` |
| 1.2 Add `enroll` to `CourseRepository` (composeApp) | NOT IN SLICE 1 SCOPE | Belongs to slice 2 (app repository). Intentionally unchecked; not a critical finding for this slice. |
| 1.3 Enrollment in `CourseService` (idempotent insert + return UserProgress) | DONE | `CourseService.enrollOfficialCourse()` inserts only when no existing enrollment, then returns `UserProgress` |
| 1.4 `POST /courses/{id}/enroll` route (JWT-guarded) | DONE | Route added inside `authenticate("auth-jwt")` block in `courseRoutes.kt`; uses `currentUserId()` |
| 1.5 Compute `exerciseCount` via `COUNT(exercises)` in `getCourseById()` | DONE | Single `Lessons leftJoin Exercises` with `groupBy` and `Exercises.id.count()` — non-N+1 |
| 1.6 Extend `ServerIntegrationTest` (success, 401, 404, non-official, already-enrolled, exerciseCount) | DONE | Two new `@Test`s cover all six required cases |

## Build / Test / Coverage Evidence

| Check | Command | Result |
|---|---|---|
| Slice-1 targeted tests | `./gradlew :server:test --tests ...official enrollment route handles success auth errors and idempotency --tests ...course detail route includes lesson exercise counts` | BUILD SUCCESSFUL, both tests pass |
| Full server suite | `./gradlew :server:test` | BUILD SUCCESSFUL |
| Shared module compiles | `./gradlew :shared:jvmJar :composeApp:compileKotlinMetadata` | BUILD SUCCESSFUL; `exerciseCount` field is backward-compatible via default `0` |

Note: `:server:compileTestKotlin` shows `UP-TO-DATE`, meaning the test compiles against the modified service and routes without regressions in existing `ServiceLayerTest` / `AdminServiceTest` which use the default-arg `CourseService()` constructor.

## Spec Compliance Matrix

### course-enrollment

| Scenario | Required Behavior | Status | Covering Test |
|---|---|---|---|
| Successful enrollment in official course | 200 + `UserProgress` containing course id in `enrolledCourseIds` | PASS | `firstEnrollment` asserts 200 and `"official-enroll" in firstProgress.enrolledCourseIds` |
| Enrollment in non-existent course | 404 NotFound | PASS | `notFound` asserts 404 |
| Unauthenticated enrollment attempt | 401 Unauthorized | PASS | `unauthorized` (no bearerAuth) asserts 401 |
| Enrollment in non-official course | 400 BadRequest | PASS | `nonOfficial` asserts 400 |
| Already-enrolled user | 200 with current `UserProgress`, no duplicate row | PASS | `secondEnrollment` 200, equal progress, DB `count()` == 1 |
| Client Enrollment Repository (3 sub-scenarios) | repository method calls endpoint + Bearer | OUT OF SLICE 1 | Slice 2 (app repository) — covered by slice-2 report |
| Enrollment CTA Replaces Visual-Only Button (3 sub-scenarios) | UI calls endpoint, updates local state, shows error | OUT OF SLICE 1 | Slice 3 (UI) |

### lesson-display

| Scenario | Required Behavior | Status | Covering Test |
|---|---|---|---|
| Lesson carries exercise count | serialized JSON contains `exerciseCount` with correct int | PASS (server side) | Course body deserialized asserts `[0].exerciseCount == 0`, `[1].exerciseCount == 2` |
| Client deserializes exercise count | client `Lesson` retains `exerciseCount` | OUT OF SLICE 1 | Slice 2/3 — client-side concern; shared field default enables this |
| Default value for backward compatibility | missing field → `exerciseCount = 0` | NOT TESTED THIS SLICE | Field declares `= 0` default; covering test deferred to slice 2/3 client test (SUGGESTION) |
| Course response includes computed exercise counts | lessons carry correct counts | PASS | `course detail route includes lesson exercise counts` asserts 0 and 2 |
| Lesson with no exercises has count zero | `exerciseCount == 0` | PASS | `lesson-zero` with no exercises asserted 0 |
| Exercise Count Display on Lesson Card (2 sub-scenarios) | UI renders "N ejercicios" | OUT OF SLICE 1 | Slice 3 (UI) |

## Correctness Table

| Concern | Result |
|---|---|
| Enrollment insert is idempotent | PASS — guarded by `existingEnrollment == null` check; DB row count stays at 1 after duplicate call |
| `exerciseCount` aggregated in single query (non-N+1) | PASS — `Lessons leftJoin Exercises` + `groupBy` + `Exercises.id.count()` |
| Route is auth-guarded | PASS — endpoint lives inside `authenticate("auth-jwt")`; `currentUserId()` returns null without token → 401 |
| Non-official course rejected | PASS — `BadRequest` with `"Non-official courses require a join code"` matches spec wording |
| `Lesson.exerciseCount` is backward-compatible | PASS — default `= 0` plus optional `exercises` list; existing call sites still compile |
| `CourseService` default constructor preserved | PASS at slice 1; RESOLVED in slice 2 — `Main.kt` now passes the shared `userService` |

## Design Coherence Table

| Design Decision | Implementation | Coherent? |
|---|---|---|
| Local router isolated to ACTIVITIES tab | n/a (slice 3) | SKIPPED — out of slice |
| Dedicated `CourseDetailViewModel` | n/a (slice 3) | SKIPPED — out of slice |
| Enrollment returns `UserProgress` | `CourseEnrollmentResult.Success(progress: UserProgress)`; route responds the object | YES |
| Embed `exerciseCount` aggregated server-side | `getCourseById` computes via leftJoin+count; `toLesson` accepts `exerciseCount` param | YES |
| `UserService.readUserProgress` reused | Promoted from `private` to `internal`; `CourseService` delegates | YES |

## Issues

### CRITICAL
- None.

### WARNING (slice 1)
- **W1-slice1 — `CourseService` instantiates its own `UserService` instead of receiving the shared instance.** RESOLVED in slice 2: `server/Main.kt` now constructs `CourseService(userService)` and `:server:test` still passes.

### SUGGESTION (slice 1)
- **S1 — Add a slice-2/3 client test for `exerciseCount` default-on-missing-field.** Still open after slice 2; tracked in slice-2 S2.
- **S2 — Spec example counts (5, 3, 7) vs. test counts (0, 2).** Behaviorally equivalent.

## Final Verdict (Slice 1)

**PASS** — preserved. Slice-1 composition warning was resolved safely in slice 2.