# Post-Apply Gate Report (PR 2 / Unit 2): architecture-refactor-assessment

> **Gate type**: Post-apply implementation gate for PR 2 (server service layer extraction, Phase 4 tasks 4.1–4.12).
> Phase 5 (test hardening) is explicitly out of scope for this slice.
> PR 1 (composeApp DI) changes coexist in the working tree but are not the focus of this report.

- **Change**: `architecture-refactor-assessment`
- **Slice**: PR 2 / Work Unit 2 — Server service layer
- **Artifact store mode**: `openspec`
- **Artifacts inspected**: `proposal.md`, `design.md`, `tasks.md`, `verify-report.md` (design gate)
- **Source inspected**: `server/.../service/{CourseService,AuthService,UserService,LessonService,ExerciseService,ServiceMappers}.kt`,
  `server/.../routes/{course,auth,user,lesson,exercise}Routes.kt`, `server/.../Main.kt`,
  `server/.../plugins/Security.kt`, `server/src/test/.../ServerIntegrationTest.kt`.
- **Diff baseline**: `HEAD` (commit `e1a3f3d`) for behavioral comparison of all 5 route files + `Main.kt`.

## 1. Contract Conformance (scope match)

| Aspect | Status | Evidence |
|---|---|---|
| Server-only scope | PASS | All PR 2 source edits live under `server/src/main/kotlin/...`. No `composeApp/`, `shared/`, or `gradle/` files were touched by PR 2. |
| Tasks artifact updated | PASS | `tasks.md` Phase 4 items 4.1–4.12 all marked `[x]`; Phase 5 left `[ ]` as expected. |
| 5 service classes created | PASS | `CourseService`, `AuthService`, `UserService`, `LessonService`, `ExerciseService` all present under `service/`. |
| Routes accept service param | PASS | All 5 route functions now take a `*Service` parameter (`courseRoutes(service: CourseService)`, etc.). |
| `Main.kt` wires services | PASS | `Main.kt` instantiates the 5 services once in `module()` and passes each to its route registration function. |
| Fetch-then-authorize pattern (task 4.6) | PASS | `courseRoutes` PUT/DELETE use `service.getCreatorId(id)` + `requireSelfOrAdmin(creatorId)` exactly as specified in task 4.6 and design-gate W3 resolution. |

Verdict: **PASS** — implemented scope matches tasks/design and is server-only plus tasks artifact.

## 2. Hallucination Check

All service classes, methods, and route function changes are real and coherent against the existing codebase.

| Claim | Verified |
|---|---|
| `service/CourseService.kt` 9 methods (4.1) | All present: `getOfficialCourses`, `getCourseById`, `getCoursesByCreator`, `getEnrolledCourses`, `createCourse`, `updateCourse`, `deleteCourse`, `joinCourse`, `getCreatorId`. |
| `service/AuthService.kt` 3 methods (4.2) | All present: `findUserByEmail`, `createUser`, `validateCredentials`. |
| `service/UserService.kt` (4.3) | `getUserById`, `getUserProgress`, `updateProgress` present; plus `updateUser` (necessary for PUT /users/{id}, see §5). |
| `service/LessonService.kt` (4.4) | All 5 named methods present; plus `getCourseCreatorId` + `getCreatorId` for route auth lookups. |
| `service/ExerciseService.kt` (4.5) | `getExercisesByLessonId` present; `checkAnswer`/`submitResult` from task wording NOT implemented (no corresponding route exists — see §5 W1). CRUD methods added to match actual `exerciseRoutes` operations. |
| Exposed tables referenced | All exist: `Courses`, `Lessons`, `Exercises`, `Users`, `EnrolledCourses`, `CompletedLessons`, `UserProgress`. |
| `dbQuery` helper | `com.example.proyectofinal.database.dbQuery` exists and is used inside every service method. |
| Route extension functions | All 5 `Application.*Routes(service)` signatures compile and are registered in `Main.kt`. |

No hallucinated paths, classes, or methods. Verdict: **PASS**.

## 3. Behavior Preservation

Compared each of the 5 route files against `HEAD` version. All HTTP paths, auth checks, status-code mappings, and DTO shapes are preserved.

| Route file | Endpoints | Status codes preserved | Auth preserved | DTO shape preserved |
|---|---|---|---|---|
| `courseRoutes.kt` | 8 (official, byId, byCreator, enrolled, create, update, delete, join) | yes (400/404/204/404-join) | yes (`requireSelfOrAdmin`) | yes (`Course` via `toCourse()`) |
| `authRoutes.kt` | 2 (register, login) | yes (403 ADMIN, 409 conflict, 401 invalid creds) | n/a (public) | yes (`AuthResponse(token, user)`) |
| `userRoutes.kt` | 4 (getUser, updateUser, getProgress, postProgress) | yes (400/404/401/403 role-change) | yes (`requireSelfOrAdmin`, admin-only role change) | yes (`User`, `UserProgress`) |
| `lessonRoutes.kt` | 5 (list, byId, create, update, delete) | yes (400/404/204) | yes (`requireSelfOrAdmin`, LEARNER answer hiding) | yes (`Lesson` with `exercises`) |
| `exerciseRoutes.kt` | 4 (list, create, update, delete) | yes (400/404/204) | yes (`requireSelfOrAdmin`, LEARNER answer hiding) | yes (`Exercise`) |

### Subtle behavior notes (none are regressions)
- **`getEnrolledCourses` empty-list short-circuit**: original ran `Courses.selectAll().where { Courses.id inList emptyList() }` when a user had no enrollments; new code explicitly returns `emptyList()` before the query. Functionally equivalent (and safer against any Exposed `inList empty` edge behavior).
- **`Course.lessons` default**: original `GET /courses/official` and `GET /courses/creator` constructed `Course` without `lessons` (relying on default `emptyList()`); new `toCourse()` passes `lessons = emptyList()` default. Equivalent.
- **`AuthService.findUserByEmail`** now returns `AuthUserRecord?` instead of raw `ResultRow`, but routes only use the null check — equivalent.
- **`AuthService.validateCredentials`** folds the bcrypt verify + User construction that the old `login` route did inline — equivalent error mapping (null → 401 "Invalid email or password").

Verdict: **PASS** — no auth, status, or contract regressions identified.

## 4. Data Access Boundary

| Check | Result |
|---|---|
| Exposed imports / `dbQuery` calls in `routes/` | None. `grep` for `dbQuery\|org.jetbrains.exposed\|Courses\.\|Lessons\.\|...` across `routes/` returned zero real matches (only false positive was the substring inside the `getUserProgress` method name). |
| All `dbQuery` usage | Confined to `database/DbQuery.kt` (definition) and the 5 service files. |
| Route responsibilities | HTTP parsing (`call.parameters`, `call.receive`), auth (`requireSelfOrAdmin`, `currentRole`), status-code mapping, and single `service.*` calls. Routes are thin. |

Verdict: **PASS** — routes contain no Exposed queries; logic lives in services inside `dbQuery`.

## 5. Task Completion Matrix

| Task | Status | Notes |
|---|---|---|
| 4.1 `CourseService` full method set | DONE | All 9 methods present. |
| 4.2 `AuthService` | DONE | `findUserByEmail`, `createUser`, `validateCredentials`. |
| 4.3 `UserService` | DONE | Plus `updateUser` (required by PUT /users/{id}); task wording under-listed it. |
| 4.4 `LessonService` | DONE | Plus 2 ownership-lookup helpers for route auth. |
| 4.5 `ExerciseService` | DONE-WITH-DEVIATION | `checkAnswer`/`submitResult` from task wording are NOT implemented because no corresponding route exists. CRUD methods added to match actual `exerciseRoutes`. See W1. |
| 4.6 `courseRoutes` + fetch-then-authorize | DONE | `service.getCreatorId(id)` + `requireSelfOrAdmin()` exactly as specified. |
| 4.7 `authRoutes` | DONE | |
| 4.8 `userRoutes` | DONE | |
| 4.9 `lessonRoutes` | DONE | |
| 4.10 `exerciseRoutes` | DONE | |
| 4.11 `Main.kt` service wiring | DONE | |
| 4.12 Verify compile + integration tests | DONE | `./gradlew :server:test --rerun-tasks` → BUILD SUCCESSFUL, 11 tests, 0 failures. |

Phase 5 tasks (5.1–5.7) intentionally unchecked — out of scope for this slice per orchestrator instructions.

## 6. Build / Test / Coverage Evidence

| Command | Result |
|---|---|
| `./gradlew :server:test --rerun-tasks --console=plain` | **BUILD SUCCESSFUL in 1m 30s**. 8/8 tasks executed (full recompile of `:server:compileKotlin` including new `service/` package, `:server:compileTestKotlin`, `:server:test`). |
| Test report `server/build/test-results/test/TEST-com.example.proyectofinal.ServerIntegrationTest.xml` | `tests="11" skipped="0" failures="0" errors="0"`. |
| Coverage | Not collected — no coverage plugin configured for `:server`; Phase 5 task 5.7 is the planned full-suite verification step. |

The existing `ServerIntegrationTest` exercises the routes via `module(initDatabase = false, seedData = ...)` with `testApplication` + in-memory H2. Because `module()` now instantiates the services and registers routes with service params internally, the test required no signature changes and passes against the refactored code — this is real runtime evidence that route behavior, auth, serialization, and persistence are preserved end-to-end.

## 7. Design Coherence

| Design decision | Implementation alignment |
|---|---|
| Services own Exposed queries inside `dbQuery` | Aligned — every service method wraps its queries in `dbQuery`. |
| Routes keep HTTP/auth/status-code mapping only | Aligned — routes are thin. |
| `Main.kt` instantiates services once, passes to route registration | Aligned. |
| Fetch-then-authorize: service exposes `getCreatorId(id)`, route does auth (W3 resolution) | Aligned — implemented exactly as the design-gate recommended. |
| No Koin in server (design decision "Use Koin only in `composeApp`") | Aligned — services are plain classes instantiated in `module()`. |

### Design deviation (non-blocking)
- **`service/ServiceMappers.kt`** (51 lines) is an extra file not listed in `design.md` File Changes. It centralizes the `ResultRow.toCourse()/toLesson()/toExercise()/toUser()` mappers as `internal` helpers shared across services. This is a reasonable DRY refinement that keeps mapper logic out of route-owned code; it does not violate any design decision. Recorded as S1.

## 8. Findings

### CRITICAL
None.

### WARNING

- **W1 — Task 4.5 method names do not match implementation (task-spec accuracy, not a code defect).**
  Task 4.5 lists `getExercisesByLessonId`, `checkAnswer`, `submitResult` for `ExerciseService`.
  `checkAnswer` and `submitResult` do not correspond to any existing route in `exerciseRoutes.kt`
  (and `submitResult` would overlap with the `/progress` POST owned by `userRoutes`). The
  implementer correctly mapped the actual route operations (`createExercise`, `updateExercise`,
  `deleteExercise`, `getLessonCreatorId`, `getCreatorId`) instead of inventing the absent methods.
  This is the right call, but the task spec should be corrected before archive so the artifact
  set is internally consistent. No code change required.

### SUGGESTION

- **S1 — `ServiceMappers.kt` is an undocumented design addition.** Consider adding a one-line
  note to `design.md` File Changes (or accepting it as an implementation detail) so the artifact
  set reflects the actual file inventory. Non-blocking; the file is `internal` and well-scoped.

- **S2 — `UserService.updateUser` was added beyond task 4.3's named method set.** This is
  necessary (PUT /users/{id} requires it) and correct. Task 4.3's wording should be expanded to
  include `updateUser` before archive for the same reason as W1.

- **S3 — Unrelated working-tree change `openspec/changes/plataforma-aprendizaje-matematica/specs/learning/spec.md`** is modified but is NOT part of PR 2's scope. Ensure this file is excluded
  from the PR 2 commit (or moved to its own change) to keep the slice reviewable. No `server/`
  impact.

- **S4 — No dedicated service-layer unit tests yet.** Phase 5 (tasks 5.4–5.6) is the planned
  home for `CourseServiceTest` / `AuthServiceTest` / `LessonServiceTest` / `ExerciseServiceTest`.
  The current `ServerIntegrationTest` provides end-to-end route coverage (11 passing) which is
  sufficient to gate PR 2, but service-isolated coverage will land in PR 3.

## 9. Final Verdict

**PASS WITH WARNINGS**

PR 2 (server service layer extraction) is complete and correct:
- All Phase 4 tasks 4.1–4.12 are implemented and verified.
- The slice is strictly server-only; no `composeApp`, `shared`, or `gradle` changes were
  introduced by PR 2.
- Routes are behavior-preserving (auth, status codes, DTO shapes, fetch-then-authorize) —
  proven by 11 passing integration tests against the refactored `module()`.
- The data-access boundary is clean: zero Exposed references in `routes/`; all queries live
  inside services within `dbQuery`.
- The one WARNING (W1) is a task-spec wording mismatch, not a code defect — the implementation
  made the correct decision.

## 10. Recommendation

**proceed-next-slice**

PR 2 is ready to commit as the server service-layer slice. Before archive (after PR 3),
reconcile the task-spec wording issues (W1, S2) and decide whether `ServiceMappers.kt`
needs a design-note (S1). Proceed to PR 3 (test hardening, Phase 5).

## verification commands

```bash
# Compile + run existing server integration tests (forces fresh recompile)
./gradlew :server:test --rerun-tasks --console=plain
# Result: BUILD SUCCESSFUL in 1m 30s; tests=11 skipped=0 failures=0 errors=0

# Confirm no Exposed/dbQuery leakage into routes
grep -rn "dbQuery\|org.jetbrains.exposed\|Courses\.\|Lessons\.\|Exercises\.\|Users\.\|EnrolledCourses\." \
  server/src/main/kotlin/com/example/proyectofinal/routes/
# Result: zero real matches (only false positive inside method name getUserProgress)
```

## skill_resolutions

- `sdd-verify` SKILL.md loaded. Strict TDD module **not** loaded — no Strict TDD mode signal
  from orchestrator or project config; standard verification path used.
- `_shared` SDD references: not separately loaded; structured status provided inline by the
  orchestrator (artifact paths + context files + assigned scope).
- Persistence: report written to openspec artifact store as
  `openspec/changes/architecture-refactor-assessment/verify-report-pr2.md` (separate from the
  pre-existing design-phase `verify-report.md` to preserve both gates).
