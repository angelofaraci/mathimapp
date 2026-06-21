## Exploration: Lesson Progress Tracking Based on Exercise/Level Completion

### Current State

The system already has a coarse progress model, but it is lesson-level only:

- **Shared model**: `UserProgress` tracks `completedLessonIds: Set<String>`, `totalScore`, and `enrolledCourseIds`.
- **Server persistence**: `UserProgress` table (total score per user) and `CompletedLessons` many-to-many table. There is **no** per-exercise completion tracking.
- **Progress endpoint**: `POST /progress` accepts `CompleteLessonRequest(userId, lessonId, score)` and immediately marks the lesson as completed and adds the score.
- **App persistence**: SQLDelight mirrors the server with `UserProgressEntity` and `CompletedLesson` tables.
- **App UI**: Only a `CourseScreen` exists. There are **no** lesson-detail or exercise screens yet.
- **Schema gap**: There is no `Level` entity. Exercises are the only granular content units inside a lesson.

The product decision states that **theory content MUST NOT count toward completion**, and a lesson is completed **only when all exercises/levels in that lesson are finished**. The existing endpoint lets any caller mark a lesson complete arbitrarily, which violates the new rule.

### Affected Areas

- `shared/src/commonMain/kotlin/com/example/proyectofinal/models/Models.kt` — needs new request/response shapes for exercise completion and enriched progress.
- `server/src/main/kotlin/com/example/proyectofinal/database/Tables.kt` — needs a `CompletedExercises` table.
- `server/src/main/kotlin/com/example/proyectofinal/service/UserService.kt` — needs exercise completion logic and lesson-completion derivation.
- `server/src/main/kotlin/com/example/proyectofinal/routes/userRoutes.kt` — needs a new authenticated endpoint to submit exercise completion.
- `server/src/test/kotlin/.../ServerIntegrationTest.kt` and `ServiceLayerTest.kt` — need new tests for the completion flow.
- `composeApp/src/commonMain/sqldelight/.../AppDatabase.sq` — needs `CompletedExercise` table and queries.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/UserApi.kt` — needs new API call for exercise completion.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/KtorUserRepository.kt` — needs to sync exercise completions locally.
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/UserRepository.kt` — needs progress-related methods (currently absent from the interface).
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — may need adapter registration for new enum/table types.

### Approaches

1. **Server-Driven Auto-Completion (Recommended)**
   - Add `CompletedExercises(userId, exerciseId)` table.
   - Client POSTs `CompleteExerciseRequest(userId, exerciseId, score)` to a new endpoint.
   - Server inserts the exercise completion, sums the score into `UserProgress.totalScore`, then counts whether all exercises for the parent lesson are completed. If so, it auto-inserts into `CompletedLessons`.
   - `GET /progress/{userId}` remains the read endpoint but now derives lesson completion from exercise data.
   - **Pros**: single source of truth, rule is enforced server-side, works across devices, enables future teacher dashboards with partial progress.
   - **Cons**: requires new table, endpoint, and service logic; slightly higher initial effort.
   - **Effort**: Medium

2. **Client-Driven Completion**
   - Client tracks exercise completions locally (or via a lightweight sync endpoint without server-side lesson derivation).
   - When the client detects all exercises for a lesson are done, it calls the existing `POST /progress` with `CompleteLessonRequest`.
   - **Pros**: minimal server changes (could reuse existing `CompletedLessons` table and endpoint).
   - **Cons**: two devices can diverge, no server-side partial progress, completion rule lives in client code, harder for teachers to monitor.
   - **Effort**: Low

3. **Hybrid Explicit Completion**
   - Server stores `CompletedExercises` and exposes them.
   - Client POSTs each exercise completion, then explicitly POSTs lesson completion after fetching progress and verifying all exercises are done.
   - **Pros**: flexible, server has full data.
   - **Cons**: client must orchestrate two-step flow, risk of race conditions or inconsistent state if client logic has bugs.
   - **Effort**: Medium

### Recommendation

**Adopt Approach 1 (Server-Driven Auto-Completion).**

Reasoning:
- The server is already the source of truth for courses, lessons, exercises, and enrollment. Progress should follow the same pattern.
- The completion rule ("all exercises must be done") is a domain invariant. Invariants belong in the backend.
- The existing `POST /progress` endpoint semantics conflict with the new rule. Moving to exercise-based submission makes the old direct lesson-marking obsolete or migratable.
- This enables future features (teacher view of partial lesson progress, analytics, retry logic) without architectural rework.

### Risks

- **Breaking change for `POST /progress`**: Existing clients (or tests) that call `POST /progress` with `CompleteLessonRequest` will still work mechanically but will bypass the new exercise-completion rule. We should either deprecate the direct lesson-marking behavior for learners or restrict it to admins/teachers.
- **SQLDelight schema migration**: Adding `CompletedExercise` to the local database requires a schema version bump. For development this is usually a fresh install, but if the app is already distributed, a migration script is needed.
- **No exercise UI exists**: The app currently has no screens to actually do exercises. The progress-tracking backend can be built, but it will not be verifiable end-to-end until lesson/exercise UI screens are created. This is a delivery dependency, not a blocker for the backend work.
- **Assumption on levels**: There is no `Level` entity. If "levels" are introduced later as a grouping above exercises, the `CompletedExercises` model may need a `levelId` column. For now, exercises are treated as the atomic completion unit.

### Ready for Proposal

**Yes.**

The orchestrator should tell the user:
- The backend will gain a new `CompletedExercises` table and endpoint.
- Lesson completion will become **derived** (all exercises done) rather than **directly posted**.
- The app-side SQLDelight schema and `UserRepository` interface will need updates to sync exercise completions.
- A separate UI work stream (lesson detail + exercise screens) is needed to actually consume this progress data, but it can be scoped independently.
