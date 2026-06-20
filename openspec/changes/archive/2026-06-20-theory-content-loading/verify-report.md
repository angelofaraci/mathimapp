# Verification Report: theory-content-loading

## Change

- Change: `theory-content-loading`
- Mode: Standard (Strict TDD: false)
- Persistence: openspec
- Delivery: stacked PR slice (PR 1 backend + PR 2 frontend, both applied)
- Verdict: **PASS WITH WARNINGS**

## Artifacts Reviewed

| Artifact | Path | Status |
|---|---|---|
| Proposal | `openspec/changes/theory-content-loading/proposal.md` | Read |
| Design | `openspec/changes/theory-content-loading/design.md` | Read |
| Tasks | `openspec/changes/theory-content-loading/tasks.md` | Read |
| Apply progress | `openspec/changes/theory-content-loading/apply-progress.md` | Read |
| Delta spec | `openspec/changes/theory-content-loading/specs/backend-auth-security/spec.md` | Read |
| Delta spec | `openspec/changes/theory-content-loading/specs/client-server-contract/spec.md` | Read |
| Full spec | `openspec/specs/theory-management/spec.md` | Read |
| Full spec | `openspec/specs/school-year-filtering/spec.md` | Read |
| Implementation | shared / server / composeApp sources and tests | Inspected |

## Completeness

| Dimension | Total | Complete | Pending | Result |
|---|---|---|---|---|
| Tasks | 20 | 20 | 0 | PASS |
| Phase 1 Shared contracts | 2 | 2 | 0 | PASS |
| Phase 2 Backend | 7 | 7 | 0 | PASS |
| Phase 3 Frontend | 7 | 7 | 0 | PASS |
| Phase 4 Testing & verification | 4 | 4 | 0 | PASS |

No unchecked implementation tasks. Task completeness is fully satisfied.

## Build / Test / Coverage Evidence

| Command | Result | Notes |
|---|---|---|
| `./gradlew :server:test :composeApp:jvmTest --rerun-tasks --console=plain` | **BUILD SUCCESSFUL in 1m 53s** | 24 actionable tasks executed |
| `:server:test` | PASS | 20 tests, 0 failures, 0 errors |
| `:composeApp:jvmTest` | PASS | 28 tests, 0 failures, 0 errors |
| Total runtime tests | PASS | **48 tests, 0 failures, 0 errors** |

### Server test suite breakdown (all 0 failures / 0 errors)

- `AuthServiceTest` — 2 tests
- `CourseServiceTest` — 2 tests (includes school-year filtering + `schoolYear` persistence)
- `LessonExerciseServiceTest` — 3 tests (includes `theory updates persist only for allowed roles and scopes`)
- `ServerIntegrationTest` — 13 tests (includes `theory route enforces auth scope and path body validation`, `official courses support school year filtering and reject invalid filters`, `authorized user can fetch official courses`)

### composeApp jvmTest suite breakdown (all 0 failures / 0 errors)

- `AppModuleTest` — 1 test (resolves new `CourseEntity.schoolYear` adapter via DI)
- `CourseViewModelTest` — 2 tests
- `KtorCourseRepositoryTest` — 8 tests (includes `getOfficialCourses sends schoolYear filter and saves it to DB`)
- `KtorExerciseRepositoryTest` — 4 tests
- `KtorLessonRepositoryTest` — 6 tests (includes `updateTheory sends shared request and refreshes lesson cache`)
- `KtorUserRepositoryTest` — 5 tests
- `NetworkClientTest` — 2 tests

### Commands not run

- `:composeApp:androidUnitTest` — not executed. SQLDelight `CourseEntity.schoolYear` adapter and schema are already validated on the JVM SQLDelight driver through `jvmTest` (write + read of `schoolYear` via the int adapter in `getOfficialCourses sends schoolYear filter and saves it to DB`). Android-specific driver validation is recommended as a follow-up only if an Android SDK environment is available; it is not required to prove the spec scenarios.

## Spec Compliance Matrix

### Delta: backend-auth-security — Requirement: Theory Mutation Authorization

| Scenario | Status | Covering Test (passed at runtime) | Evidence |
|---|---|---|---|
| Admin updates official lesson theory | PASS | `ServerIntegrationTest.theory route enforces auth scope and path body validation`; `LessonExerciseServiceTest.theory updates persist only for allowed roles and scopes` | `adminSuccess` 200, body `theoryContent == "Admin theory"`, DB row persisted; `adminResult` is `Success` |
| Teacher is limited to own courses | PASS | Same two tests | `forbidden` 403 for `otherTeacherToken`; `forbiddenTeacherResult == Forbidden`; also positive `teacherSuccess` 200 + `teacherResult Success` for own course |
| Missing authentication is rejected | PASS | `ServerIntegrationTest.theory route enforces auth scope and path body validation` | `unauthorized` (no bearer) → 401; route is inside `authenticate("auth-jwt")` plus explicit `currentUserId`/`currentRole` null → 401 |

### Delta: client-server-contract — Requirement: Shared Course School Year Field

| Scenario | Status | Covering Test (passed at runtime) | Evidence |
|---|---|---|---|
| Course carries school year | PASS | `KtorCourseRepositoryTest.getOfficialCourses sends schoolYear filter and saves it to DB`; `ServerIntegrationTest.authorized user can fetch official courses` | Client parses `courses[0].schoolYear == 3`; server returns `schoolYear == 3` |
| Course data keeps school year through the client | PASS | `KtorCourseRepositoryTest.getOfficialCourses...` | SQLDelight cache write/read: `dbCourse.schoolYear == 3`; `insertCourseToLocal` passes `schoolYear` |

### Delta: client-server-contract — Requirement: Shared Theory Update Request

| Scenario | Status | Covering Test (passed at runtime) | Evidence |
|---|---|---|---|
| Theory update request is shared | PASS | `KtorLessonRepositoryTest.updateTheory sends shared request and refreshes lesson cache` | Decoded request body equals `expectedRequest` (`TheoryUpdateRequest(lessonId, theoryContent)`) |
| Server receives theory content unchanged | PASS | `ServerIntegrationTest.theory route enforces auth scope and path body validation` | Server deserializes `TheoryUpdateRequest` and persists `"Admin theory"` / `"Teacher theory"` in `Lessons.theoryContent` |

### Full spec: theory-management — Requirement: Theory Content Read Access

| Scenario | Status | Covering Test (passed at runtime) | Evidence / Note |
|---|---|---|---|
| Lesson theory is returned | PASS (suggestion) | `ServerIntegrationTest.learner content hides correct answers` | `GET /lessons/{id}` returns full `Lesson` including `theoryContent` (mapper includes it). Test deserializes the lesson but asserts exercise masking, not the `theoryContent` value explicitly |
| Inaccessible lesson is blocked | **WARNING — UNTESTED / NOT IMPLEMENTED (known prior gate)** | None | `GET /lessons/{id}` performs no read-access/ownership/enrollment check; any authenticated user can read any lesson's theory. Design explicitly scoped read-access enforcement out of this change |

### Full spec: theory-management — Requirement: Theory Content Update Scope

| Scenario | Status | Covering Test (passed at runtime) | Evidence |
|---|---|---|---|
| Admin updates official lesson theory | PASS | `LessonExerciseServiceTest.theory updates persist only for allowed roles and scopes`; `ServerIntegrationTest.theory route...` | `adminResult Success` + persisted; `adminSuccess` 200 |
| Teacher is limited to own courses | PASS | Same tests | `teacherResult Success` (own) + `forbiddenTeacherResult Forbidden` (other teacher); `teacherSuccess` 200 + `forbidden` 403 |

### Full spec: school-year-filtering — Requirement: Official Courses Are Filtered By School Year

| Scenario | Status | Covering Test (passed at runtime) | Evidence |
|---|---|---|---|
| Matching official courses are returned | PASS | `ServerIntegrationTest.official courses support school year filtering and reject invalid filters`; `CourseServiceTest.course service query methods return persisted data` | `filteredResponse` → `[official-year-3]`; `getOfficialCourses(3)` → `[official-course-year-3]` |
| No matches returns empty list | PASS | Same tests | `emptyResponse` body empty; `getOfficialCourses(6).isEmpty()` |
| Invalid school year is rejected | PASS | `ServerIntegrationTest.official courses support school year filtering and reject invalid filters` | `invalidResponse` (`schoolYear=third`) → 400 BadRequest |

## Correctness Table

| Requirement | Source | Verdict | Rationale |
|---|---|---|---|
| Theory Mutation Authorization | delta backend-auth-security | PASS | All 3 scenarios covered by passing integration + service tests |
| Shared Course School Year Field | delta client-server-contract | PASS | Both scenarios covered by passing repository + integration tests |
| Shared Theory Update Request | delta client-server-contract | PASS | Both scenarios covered by passing repository + integration tests |
| Theory Content Read Access | full theory-management | PARTIAL | "Lesson theory is returned" PASS; "Inaccessible lesson is blocked" NOT IMPLEMENTED / UNTESTED (known prior gate) |
| Theory Content Update Scope | full theory-management | PASS | Both scenarios covered by passing service + integration tests |
| Official Courses Filtered By School Year | full school-year-filtering | PASS | All 3 scenarios covered by passing integration + service tests |

## Design Coherence Table

| Design Decision | Implementation Match | Verdict |
|---|---|---|
| Keep theory inline on `Lessons.theoryContent` (no `Topic`/`TheoryContent` table) | Yes — `Lessons.theoryContent` reused; no new entity | COHERENT |
| Put `TheoryUpdateRequest` in `shared` | Yes — `shared/.../models/Models.kt` | COHERENT |
| `GET /courses/official?schoolYear={year}`, non-numeric → 400 | Yes — `courseRoutes` parses `toIntOrNull`, 400 on parse failure | COHERENT |
| Authorize theory separately: ADMIN official only, TEACHER own only, unauth JWT challenge | Yes — `LessonService.updateTheoryContent` + route role gate | COHERENT |
| `PUT /lessons/{id}/theory` validates `pathId == body.lessonId`, returns updated `Lesson` | Yes — route returns 400 on mismatch; returns `Lesson` on success | COHERENT |
| Backend changes land first; frontend consumes stable payloads | Yes — both slices applied; contracts aligned | COHERENT |

**Functional design deviations: None.**

## Issues

### CRITICAL
None.

### WARNING

1. **Known prior backend gate — theory-management read-access not enforced.**
   - Spec: `openspec/specs/theory-management/spec.md` → Requirement "Theory Content Read Access" → Scenario "Inaccessible lesson is blocked" requires the system to reject a request for a lesson the user cannot access.
   - Implementation: `server/.../routes/lessonRoutes.kt` `GET /lessons/{id}` only checks authentication and hides answers for `LEARNER`; it performs no ownership/enrollment/role read-access check. Any authenticated user can read any lesson's `theoryContent`.
   - Tests: No covering test exists for this scenario.
   - Severity rationale: Strictly an UNTESTED spec scenario, but this is a carried-forward gap that the design explicitly scoped out of this change (design's auth decisions cover theory mutation only, not reads). It is a pre-existing spec requirement, not a regression introduced by this change. Flagged as WARNING per the known prior gate, but it remains a real unmet requirement in the canonical `theory-management` spec.

2. **Database migration not provided for `courses.school_year`.**
   - Implementation relies on `SchemaUtils.create` (per `apply-progress.md`). Pre-existing `courses` tables in persistent environments will not gain the `school_year` column without an explicit migration. In-memory/test databases are fine; production-style persistent deployments need a migration script.
   - Severity: WARNING (carried forward from apply-progress; design's migration section flagged the need).

### SUGGESTION

1. **Design/proposal file-table drift.** The proposal and design file tables list `server/.../models/LessonDto.kt` as modified for "Theory update request/response shapes". The actually modified server DTO file is `server/.../models/CourseDto.kt` (added `schoolYear` to `CreateCourseRequest`/`UpdateCourseRequest`). `TheoryUpdateRequest` correctly lives in `shared` per design decision #2, so no server `LessonDto` was needed. Documentation-only drift; no code defect.

2. **No explicit assertion on `theoryContent` for the read scenario.** `ServerIntegrationTest.learner content hides correct answers` deserializes the lesson via `GET /lessons/{id}` but asserts on exercise answer masking, not the returned `theoryContent` value. Behavior is correct (the mapper includes `theoryContent`); an explicit `assertEquals("Theory", lesson.theoryContent)` would tighten coverage of the "Lesson theory is returned" scenario.

3. **Optional Android validation not run.** `:composeApp:androidUnitTest` was not executed (environment-dependent). The JVM SQLDelight path already validates the `schoolYear` adapter and schema. Run it in an Android-SDK environment as a follow-up if available.

## Final Verdict

**PASS WITH WARNINGS**

- All delta spec scenarios (`backend-auth-security`, `client-server-contract`) are satisfied with passing covering tests at runtime.
- All `school-year-filtering` scenarios and the `theory-management` update-scope scenarios are satisfied with passing covering tests.
- 20/20 tasks complete; no unchecked implementation tasks.
- Build + tests: 48 tests, 0 failures, 0 errors.
- Two WARNINGs remain: (1) the known prior `theory-management` read-access gate is not implemented and has no covering test; (2) no DB migration for `courses.school_year` in persistent environments. Neither is a regression introduced by this change; both are carried-forward gaps.

## Archive Readiness

**Archive MAY proceed with a tracked caveat.**

- The two delta specs this change actually delivers (`backend-auth-security`, `client-server-contract`) are fully satisfied and may be archived.
- `school-year-filtering` is fully satisfied and may be archived.
- `theory-management` is partially satisfied: the update-scope requirement passes, but the read-access "Inaccessible lesson is blocked" scenario remains unmet. Do NOT mark `theory-management` read-access as satisfied during archive. Recommend opening a follow-up SDD change to implement lesson read-access enforcement (ownership/enrollment/role gating on `GET /lessons/{id}`) before the `theory-management` capability is considered complete.
- The DB migration gap should be tracked as a separate operational/deployment task.
