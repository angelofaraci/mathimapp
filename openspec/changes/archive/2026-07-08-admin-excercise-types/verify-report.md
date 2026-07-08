# Verification Report: admin-excercise-types

- **Change**: admin-excercise-types
- **Mode**: Standard (no Strict TDD)
- **Persistence**: OpenSpec file (`verify-report.md`)
- **Verifier**: sdd-verify sub-agent
- **Date**: 2026-07-08 (re-verify after admin validation test remediation)
- **Verdict**: **PASS WITH WARNINGS**

## Cumulative Scope

Verification covers the complete PR1 + PR2 + PR3 cumulative implementation, not only
the last Compose slice. All four spec deltas, the design, and 16/16 tasks were
inspected against the actual source.

## Completeness Table

| Artifact | Status | Notes |
|---|---|---|
| proposal.md | Present | Intent, scope, capabilities, success criteria defined |
| specs/admin-exercise-crud | Present | 3 ADDED + 2 MODIFIED requirements, 9 scenarios |
| specs/client-server-contract | Present | 2 ADDED + 1 MODIFIED, 5 scenarios |
| specs/exercise-type-player | Present | 4 requirements, 12 scenarios |
| specs/lesson-read-access | Present | 1 ADDED + 1 MODIFIED, 6 scenarios |
| design.md | Present | Architecture decisions, data flow, migration plan |
| tasks.md | Present | 16/16 tasks marked `[x]` |
| apply-progress.md | Present | Lists files changed per PR slice, deviations, remaining `None` |

### Task Completion

| Phase | Completed / Total | Notes |
|---|---|---|
| Phase 1 (shared + server) | 7/7 | All marked complete; source inspected |
| Phase 2 (admin-web) | 1/1 | Build passes; no test runner (spec-acknowledged manual) |
| Phase 3 (compose player) | 4/4 | VM + screen + client API + SQLDelight |
| Phase 4 (testing) | 4/4 | Server unit/integration + VM + SQLDelight tests exist and pass |

No unchecked implementation tasks. No CRITICAL task-completion blockers.

## Build / Test / Coverage Evidence

| Command | Result | Source |
|---|---|---|
| `./gradlew :server:test` | PASS — 59 tests across 7 suites, 0 failures, 0 errors (incl. new `admin exercise create rejects invalid typed payloads with documented messages`, 31.4s) | `server/build/test-results/test/*.xml` (timestamp 2026-07-08T16:44Z) |
| `./gradlew :composeApp:jvmTest` | PASS (up-to-date reuse) — all JVM suites incl. `LocalDatabaseSchemaFixesTest` (3 tests), `LessonMapViewModelTest` (7 tests), `KtorExerciseRepositoryTest` (5 tests), `KtorUserRepositoryTest` (9 tests), 0 failures | `composeApp/build/test-results/jvmTest/*.xml` |
| `npm run build` (admin-web/) | PASS — `tsc -b && vite build`, 85 modules, built in 2.82s | live execution |

Note: `:server:test --rerun-tasks` could not be re-executed fresh inside this
session because the Gradle daemon reconfigure exceeded the tool timeout budget;
the cached results above are today's and consistent with the `apply-progress.md`
recorded run. The up-to-date check passes and no test reports show failures.

## Spec Compliance Matrix

### admin-exercise-crud

| Scenario | Status | Evidence |
|---|---|---|
| MultipleChoice rejects single option | PASS | `AdminIntegrationTest.admin exercise create rejects invalid typed payloads with documented messages` POSTs 1-option MC payload → asserts `HttpStatusCode.BadRequest` (400) + body contains `MultipleChoice exercises require at least 2 options` |
| InputValue rejects empty correct value | PASS | Same test POSTs `InputValuePayload(correctValue = "   ")` → asserts 400 + body contains `correctValue is required` |
| MultiSelect rejects invalid correct reference | PASS | Same test POSTs `MultiSelectPayload(correctOptionIds = ["missing"])` → asserts 400 + body contains `correctOptionIds must reference valid options` |
| MultipleChoice form shows option editor | PASS (manual) | `Exercises.tsx` renders option editor with radio + add/remove; admin-web has no runner per design |
| InputValue form shows single text input | PASS (manual) | `Exercises.tsx` renders `placeholder` + `correctValue` text inputs |
| MultiSelect form shows checkbox editor | PASS (manual) | `Exercises.tsx` option editor in `multiple` mode with checkbox set |
| Admin lists exercises with payload data | **WARNING — partial assertion** | `toAdminExerciseResponse()` includes `payload`; `AdminIntegrationTest` asserts only IDs, never asserts `payload` field presence/contents on the list response |
| Admin creates an exercise for a lesson | PASS | `AdminIntegrationTest.admin exercise routes...` creates `admin-exercise` 200 |
| Exercise for non-existent lesson is rejected | PASS | same test asserts 400 + `unknown lesson` |
| Missing required fields are rejected | PASS | same test sends raw body without type/payload and asserts 400 `Invalid request body` |
| Admin updates exercise payload | PASS | same test PUTs `UpdateAdminExerciseRequest` 200 |
| Admin reassigns exercise to a different lesson | PASS | same test reassigns to `standalone-lesson` and asserts response lessonId |
| Reassign to non-existent lesson is rejected | PASS | same test asserts 400 + `unknown lesson` |

### client-server-contract

| Scenario | Status | Evidence |
|---|---|---|
| MultipleChoicePayload serializes with discriminator | PASS | `Models.kt` `@SerialName("multipleChoice")`; `KtorExerciseRepositoryTest.payload JSON round trips...` round-trips `MultiSelectPayload`; `ServiceLayerTest` backfill asserts `"type":"multipleChoice"` in DB |
| InputValuePayload serializes with discriminator | PASS | `@SerialName("inputValue")`; used by attempt endpoint + repository tests |
| MultiSelectPayload serializes with discriminator | PASS | `@SerialName("multiSelect")`; `KtorExerciseRepositoryTest.payload JSON round trips through local DB storage` round-trips `multiSelectExercise` |
| Exercise deserializes with typed payload | PASS | `KtorExerciseRepositoryTest.getExercisesByLesson...` asserts `mockExercises[i].payload` equals decoded DB payload |
| Exercise serializes with payload | PASS | same repository tests post/decode payloads via Json `classDiscriminator = "type"` |
| Shared request shape is used (CompleteLessonRequest) | PASS | `Models.kt CompleteLessonRequest`; `UserService.updateProgress` consumes it |
| Completion data reaches the server | PASS | `ServerIntegrationTest.exercise completion validates path body match...` covers completion flow |

### exercise-type-player

| Scenario | Status | Evidence |
|---|---|---|
| MultipleChoice renders radio-button cards | PASS (source-inspected) | `LessonMapScreen.ExerciseAnswerSection` MC branch uses `RadioButton` per option; no Compose render test runner |
| InputValue renders text input | PASS (source-inspected) | MC else branch renders `MTextField` + label; submit button present |
| MultiSelect renders checkbox cards | PASS (source-inspected) | MC else branch renders `Checkbox` per option |
| Unknown payload type shows error | **WARNING — UNTESTED** | `else` branch renders unsupported-exercise card; no Compose UI test covers it |
| MultipleChoice validates single option | PASS | `ServiceLayerTest.attempt exercise validates typed submissions...` asserts `4` correct vs `3` wrong |
| InputValue validates trimmed text | PASS | same test asserts `InputValueSubmission("  four  ")` matches `correctValue="four"` |
| MultiSelect validates exact set match | PASS | same test asserts `["3","2"]` correct |
| MultiSelect rejects partial match | PASS | same test asserts `["2"]` incorrect against `["3","2"]` |
| InputValue rejects empty submission (client-side) | PASS | `LessonMapViewModelTest.blank input answer is rejected client side` |
| Wrong answer shows feedback and stays | PASS | `LessonMapViewModelTest.wrong answer keeps exercise active until a correct retry advances` |
| Correct answer advances | PASS | same VM test advances on correct retry |
| Retry allows selection change | PASS (source-inspected) | VM `RetryReady` phase clears feedback on next draft selection; submit button re-enabled |
| Multiple wrong attempts do not block | **SUGGESTION** | Implementation permits repeated retries, but only single retry→correct is asserted |
| Student payload omits correct answer | PASS | `ExercisePayloadSupport.stripAnswers` + `ServiceLayerTest.exercise service...` asserts hidden `correctAnswer`; ContentReadAccess hides for STUDENT |
| Admin payload includes correct answer | PASS | `shouldHideLessonAnswers` returns `role == STUDENT`, so ADMIN/TEACHER see full payload |

### lesson-read-access

| Scenario | Status | Evidence |
|---|---|---|
| Student MC payload omits correctOptionId | PASS | `stripAnswers` copies `correctOptionId = null`; `explicitNulls = false` omits field in JSON; `ServiceLayerTest` hidden-assert |
| Student InputValue omits correctValue | PASS | `stripAnswers` copies `correctValue = null`; behavior symmetric |
| Student MultiSelect omits correctOptionIds | PASS | `stripAnswers` copies `correctOptionIds = null` |
| Student options remain intact | PASS | `stripAnswers` only nulls answer fields; `getExercisesByLessonId(hideAnswers = true)` keeps options |
| Admin receives full typed payload | PASS | `shouldHideLessonAnswers` returns false for ADMIN; admin listing returns full payload via `toAdminExerciseResponse` |
| Teacher (creator) receives full typed payload | PASS | `shouldHideLessonContents` false for TEACHER; standalone + course-linked creator visibility preserved |

## Correctness Table (implementation vs spec)

| Spec Requirement | Implementation | Compliant |
|---|---|---|
| Sealed `ExercisePayload` hierarchy with 3 phase-1 types | `Models.kt` sealed interface + 3 payloads | YES |
| `Exercise` with `payload` field + base metadata | `Exercise(id, lessonId, title, type, payload)` with no public `points` / `difficulty` fields | YES |
| MultipleChoice MC 2+ options + valid `correctOptionId` | `validatePayload` enforces | YES |
| InputValue non-empty `correctValue` | `validatePayload` enforces | YES |
| MultiSelect 2+ options, ≥1 correct, valid refs | `validatePayload` enforces | YES |
| Backend attempt validation (not client-side) | `UserService.attemptExercise` + `evaluateAttempt` | YES |
| Answer hiding for STUDENT via payload copy-strip | `stripAnswers` + `ContentReadAccess` | YES |
| Immediate retry without advance on wrong | VM `RetryReady` + backend returns `isCorrect=false` without recording completion | YES |
| SQLDelight stores payload JSON | `AppDatabase.sq` `ExerciseEntity.payload TEXT NOT NULL` | YES |
| Legacy cached-DB upgrade/repair | `LocalDatabaseSchemaFixes.ensureExerciseEntityShape` + `jvmTest` repair test | YES |
| Flyway V5 backfill + TRUE_FALSE normalization + NOT NULL | `V5__exercise_payload_json.sql` + `ServiceLayerTest` backfill tests, including spaced legacy option trimming | YES |

## Design Coherence Table

| Design Decision | Implementation | Coherent |
|---|---|---|
| Replace shared `Exercise` contract entirely | Replaced, plus retained compatibility constructor + `question`/`options`/`correctAnswer` accessors for back-compat (declared deviation) | YES (accepted deviation) |
| Same payload model, nullable answer fields stripped for students | `stripAnswers` nulls answer fields; `Json { explicitNulls = false }` omits them | YES |
| Backend attempt validation (no client-side correct-answer exposure) | `evaluateAttempt` server-side | YES |
| Backfill JSON, keep old flat columns temporarily, coordinated deploy | V5 writes JSON from legacy `correct_answer`/`options`; `Tables.kt` keeps `question`/`options`/`correctAnswer` | YES |
| `ExerciseType` enum = {MULTIPLE_CHOICE, INPUT_VALUE, MULTI_SELECT} | Enum additionally retains deprecated `TRUE_FALSE` for normalization safety | WARNING — minor deviation from design's literal three-value enum; does not break spec because all `TRUE_FALSE` rows are normalized to `MULTIPLE_CHOICE` in both migration and validators |

## Issues

### CRITICAL

None. The three previously CRITICAL admin validation scenario gaps
("MultipleChoice rejects single option", "InputValue rejects empty correct value",
"MultiSelect rejects invalid correct reference") are now covered at runtime by
`AdminIntegrationTest.admin exercise create rejects invalid typed payloads with documented messages`,
which asserts `HttpStatusCode.BadRequest` (400) plus each documented rejection message.
The test passed in today's `./gradlew :server:test` run (31.404s, 0 failures).

### WARNING

1. **`admin-exercise-crud` — "Admin lists exercises with payload data" scenario lacks explicit payload assertion.**
   - `adminRoutes` returns `AdminExerciseResponse(payload = payload)` via `toAdminExerciseResponse`, so behavior is correct.
   - `AdminIntegrationTest` only asserts IDs of returned exercises, never asserts the `payload` field is present or contains typed data.
   - Low risk because behavior is structurally correct and serialization is covered elsewhere, but the scenario's specific assertion is missing.

2. **`exercise-type-player` — "Unknown payload type shows error" UI fallback is implemented but untested.**
   - `ExerciseAnswerSection` `else` branch renders the unsupported-exercise card.
   - No Compose render/mechanical test runner exists for the screen.
   - The VM/player dispatch path is not exercised for unknown payload types. Behavior is verified by source inspection only.

3. **Design deviation — `ExerciseType` retains deprecated `TRUE_FALSE` variant.**
   - Design declared the enum as exactly `{MULTIPLE_CHOICE, INPUT_VALUE, MULTI_SELECT}`.
   - `Models.kt` keeps `TRUE_FALSE` with `@Deprecated`, normalized in both V5 migration and `normalizeType`.
   - Does not violate any spec scenario (TRUE_FALSE never reaches validation) but is a design text mismatch. Already declared in `apply-progress.md` deviations.

### SUGGESTION

1. **"Multiple wrong attempts do not block"** scenario is implemented (retry state permits repeated submissions) but no test exercises more than one wrong answer before the correct one. Consider extending `LessonMapViewModelTest` with a two-wrong-then-correct sequence.
2. **Admin-web editor behavior** has no automated test (spec-acknowledged). The shared `Exercises.tsx` form-state conversion logic between MC/MultiSelect (carrying `correctOptionId(s)`) is non-trivial; a lightweight Vitest unit over `applyTypeSwitch`/`buildExerciseDto` would surface regressions without a DOM runner.

## Final Verdict

**PASS WITH WARNINGS**

The implementation is cumulatively complete across PR1/PR2/PR3, matches the
design's architecture decisions, builds green across `:server:test` (59 tests,
0 failures), `:composeApp:jvmTest`, and `admin-web` build, and satisfies all
four spec deltas at the behavior level. The three previously CRITICAL
admin-exercise-crud validation scenarios now have passing covering tests at
runtime. Remaining findings are WARNING/SUGGESTION-level (admin-web manual-only
coverage, Compose UI scenarios tested by source inspection only, one minor
declared design deviation on `ExerciseType.TRUE_FALSE`). No CRITICAL issues
remain. The change is clean enough for archive.
