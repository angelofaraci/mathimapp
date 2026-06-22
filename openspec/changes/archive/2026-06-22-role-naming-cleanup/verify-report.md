# Verification Report: role-naming-cleanup

**Change**: role-naming-cleanup (archived at `openspec/changes/archive/2026-06-22-role-naming-cleanup/`)
**Version**: N/A (no spec version field)
**Mode**: Standard (Strict TDD inactive — `openspec/config.yaml` `apply.tdd: false`)
**Date**: 2026-06-22 (re-verification after post-review reconciliation apply follow-up)

## Re-verification Context

This report refreshes the prior verification after a pre-commit review follow-up that:
1. Added a direct SQLDelight/local-cache legacy `"LEARNER"` row decode test in `KtorUserRepositoryTest.kt` (resolving prior SUGGESTION 1).
2. Corrected `archive-report.md` (test counts: 64 → 65; explicit note about the added direct coverage).
3. Strengthened rollback guidance in the archived `proposal.md` (concrete 3-step migration-aware recovery path, coherent with `archive-report.md`).

The implementation source (shared parser, server persistence/JWT/guards, compose adapter/repository) is unchanged from the verified state; only test coverage and SDD documentation were reinforced.

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 23 |
| Tasks complete | 23 |
| Tasks incomplete | 0 |

All 23 tasks across Phase 1–6 remain checked `[x]` in `tasks.md`. The archive step (6.2) is complete — the delta spec directory `openspec/changes/archive/2026-06-22-role-naming-cleanup/specs/backend-auth-security/` is empty (merged into the main spec).

## Build & Tests Execution

All three commands executed fresh in a single `--rerun-tasks` build (24 tasks executed, no cached results).

**Build / SQLDelight generation**: ✅ Passed
```text
$ ./gradlew :composeApp:generateSqlDelightInterface --rerun-tasks --console=plain
> Task :composeApp:generateCommonMainAppDatabaseInterface
> Task :composeApp:generateSqlDelightInterface
BUILD SUCCESSFUL
```

**Server tests**: ✅ 29 passed / 0 failed / 0 skipped
```text
$ ./gradlew :server:test --rerun-tasks --console=plain
> Task :server:test
BUILD SUCCESSFUL in 2m 6s
```
Per-class results (`server/build/test-results/test`):
- `ServerIntegrationTest`: 17 tests, 0 failures, 0 errors
- `LessonExerciseServiceTest`: 6 tests, 0 failures, 0 errors
- `CourseServiceTest`: 2 tests, 0 failures, 0 errors
- `UserServiceTest`: 2 tests, 0 failures, 0 errors
- `AuthServiceTest`: 2 tests, 0 failures, 0 errors

**Compose app tests**: ✅ 36 passed / 0 failed / 0 skipped (was 35; +1 from the new direct adapter test)
```text
$ ./gradlew :composeApp:jvmTest --rerun-tasks --console=plain
> Task :composeApp:jvmTest
BUILD SUCCESSFUL
```
Per-class results (`composeApp/build/test-results/jvmTest`):
- `UserRoleTest`: 3 (parser compatibility) — 0 failures
- `KtorUserRepositoryTest`: 8 (was 7; +`user role adapter decodes legacy learner rows and preserves canonical student rows`) — 0 failures
- `KtorCourseRepositoryTest`: 8 — 0 failures
- `KtorLessonRepositoryTest`: 6 — 0 failures
- `KtorExerciseRepositoryTest`: 4 — 0 failures
- `NetworkClientTest`: 2 — 0 failures
- `CourseViewModelTest`: 2 — 0 failures
- `ApiBaseUrlJvmTest`: 2 — 0 failures
- `AppModuleTest`: 1 — 0 failures

Two pre-existing Kotlin compiler warnings about `expect`/`actual` classes in Beta (in `DatabaseDriverFactory.kt`) are unrelated to this change and do not affect the result.

**Coverage**: ➖ Not available (config `verify.coverage_threshold: 0`; no coverage tooling configured).

**Grand total**: 65 tests passing, 0 failures, 0 skipped (29 server + 36 compose app) — consistent with the corrected `archive-report.md`.

## Spec Compliance Matrix

### backend-auth-security

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| JWT Protected Access | Authorized request succeeds | `ServerIntegrationTest > authorized user can fetch official courses` | ✅ COMPLIANT |
| JWT Protected Access | Invalid identity is rejected | `ServerIntegrationTest > protected courses route rejects missing token`, `signed token without userId is rejected` | ✅ COMPLIANT |
| Registration Role Limits | Student or teacher registration succeeds | `ServerIntegrationTest > register returns token and persisted user` | ✅ COMPLIANT |
| Registration Role Limits | Admin registration is blocked | `ServerIntegrationTest > public admin registration is rejected` | ✅ COMPLIANT |
| Protected Course And Progress Access | Unauthenticated course access is denied | `ServerIntegrationTest > protected courses route rejects missing token` | ✅ COMPLIANT |
| Protected Course And Progress Access | Progress visibility follows role | `ServerIntegrationTest > exercise completion uses authenticated learner identity and updates progress`, `lesson read route enforces visibility scopes` | ✅ COMPLIANT |
| Protected Course And Progress Access | Teacher-scoped progress access is not in scope | `ServerIntegrationTest > lesson read route enforces visibility scopes` | ✅ COMPLIANT |
| Secure Secret and Seed Handling | Secrets come from configuration | `ServerIntegrationTest > seed uses configured admin credentials hashes password and avoids secret output` | ✅ COMPLIANT |
| Secure Secret and Seed Handling | Secrets are not exposed in logs | `ServerIntegrationTest > seed uses configured admin credentials hashes password and avoids secret output` | ✅ COMPLIANT |
| Student Responses Hide Correct Answers | Student response hides answers | `ServerIntegrationTest > learner content hides correct answers` | ✅ COMPLIANT |
| Student Responses Hide Correct Answers | Hidden answers do not break content delivery | `ServerIntegrationTest > learner content hides correct answers` | ✅ COMPLIANT |
| Theory Mutation Authorization | Admin updates official lesson theory | `ServerIntegrationTest > theory route enforces auth scope and path body validation`, `LessonExerciseServiceTest > theory updates persist only for allowed roles and scopes` | ✅ COMPLIANT |
| Theory Mutation Authorization | Teacher is limited to own courses | `ServerIntegrationTest > lesson mutations require course owner or admin`, `theory route enforces auth scope and path body validation` | ✅ COMPLIANT |
| Theory Mutation Authorization | Missing authentication is rejected | `ServerIntegrationTest > protected courses route rejects missing token` (theory route shares auth pipeline) | ✅ COMPLIANT |
| Compatibility (cross-cutting) | Legacy `LEARNER` ≡ `STUDENT` on server reads (DB rows + JWT claims); new writes emit `STUDENT` | `ServerIntegrationTest > legacy learner role values still authenticate and hydrate as student` (DB row `"LEARNER"` + JWT `"LEARNER"` → `UserRole.STUDENT`, 200 OK) | ✅ COMPLIANT |
| Compatibility (cross-cutting) | Legacy `LEARNER` local-cache-row decode through production SQLDelight adapter | `KtorUserRepositoryTest > user role adapter decodes legacy learner rows and preserves canonical student rows` (raw `"LEARNER"` row via `driver.execute` → `selectUserById` through `userRoleColumnAdapter.decode` → `UserRole.STUDENT`; canonical `STUDENT` round-trip also asserted) | ✅ COMPLIANT (prior PARTIAL resolved) |
| Compatibility (cross-cutting) | Parser unit mapping | `UserRoleTest > parse accepts legacy learner value`, `parse accepts canonical student value`, `parse returns null for unknown values` | ✅ COMPLIANT |

### lesson-progress-derivation

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Lesson Completion Is Derived From Exercises | All exercises complete the lesson | `LessonExerciseServiceTest > complete exercise is first wins and completes lesson on final exercise`, `ServerIntegrationTest > exercise completion uses authenticated learner identity and updates progress` | ✅ COMPLIANT |
| Lesson Completion Is Derived From Exercises | Theory alone does not complete a lesson | Derivation logic in `UserService.completeExercise` (only inserts `CompletedLessons` when all exercises done); positive test confirms derivation. No dedicated negative test (pre-existing behavior, not altered by this rename) | ✅ COMPLIANT (behavior unchanged by this change) |
| Direct Lesson Completion Is Deprecated For Students | Student direct completion is blocked | `ServerIntegrationTest > exercise completion validates path body match and learner progress deprecation` (POST /progress → 410 Gone for students) | ✅ COMPLIANT |
| Direct Lesson Completion Is Deprecated For Students | Exercise completion triggers completion instead | `LessonExerciseServiceTest > complete exercise is first wins and completes lesson on final exercise` | ✅ COMPLIANT |

**Compliance summary**: 18/18 formal scenarios compliant; all cross-cutting compatibility facets now COMPLIANT (prior PARTIAL on local-cache decode resolved by the direct adapter test).

## Correctness (Static Evidence)

| Objective | Status | Notes |
|------------|--------|-------|
| No `UserRole.LEARNER` enum references in source | ✅ Implemented | `rg "UserRole\.LEARNER" --type kotlin` → 0 matches (exit 1). Remaining `LEARNER` string mentions are only in SDD change docs, the `Compatibility` spec section, the parser mapping, legacy compat tests, and archived history (all intentional). |
| `UserRole` enum is `ADMIN, TEACHER, STUDENT` | ✅ Implemented | `shared/.../Models.kt:14-17` |
| `User(role)` default is `UserRole.STUDENT` | ✅ Implemented | `shared/.../Models.kt:10` |
| `UserRole.parse` maps `LEARNER`+`STUDENT`→`STUDENT`, `TEACHER`→`TEACHER`, `ADMIN`→`ADMIN`, else `null` | ✅ Implemented | `shared/.../Models.kt:20-25`; matches design interface exactly |
| Server persistence mapper fails loud on unknown role | ✅ Implemented | `ServiceMappers.kt:51-52` `UserRole.parse(...) ?: error(...)`; `AuthService.kt:64-65` |
| Server JWT `currentRole` nullable on invalid claim | ✅ Implemented | `Security.kt:58` `?.let(UserRole::parse)` (preserves null-return) |
| Server writes emit canonical `STUDENT` | ✅ Implemented | `authRoutes.kt:35,49`; `AuthService.kt:27`; `UserService.kt:44`; `Tables.kt:10` default `"STUDENT"`; `UserDto.kt:10` default `UserRole.STUDENT` |
| Role guards use `UserRole.STUDENT` | ✅ Implemented | `userRoutes.kt:63,74`; `UserService.kt:64`; `LessonService.kt:98,173`; `ContentReadAccess.kt:20` |
| App SQLDelight schema default `'STUDENT'` | ✅ Implemented | `AppDatabase.sq:11` |
| App custom `ColumnAdapter` (decode=parse+error, encode=name) | ✅ Implemented | `AppModule.kt:45-51` replaces `EnumColumnAdapter`; wired at `AppModule.kt:71-73` |
| App repository fallback `UserRole.STUDENT` | ✅ Implemented | `KtorUserRepository.kt:34` |
| `shared` stays platform-agnostic | ✅ Implemented | Parser has no Exposed/Ktor/SQLDelight/platform dependency; framework adapters remain in owning modules |

## Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| Parser ownership: platform-agnostic `UserRole.parse` in `shared` | ✅ Yes | Single parser reused by server and app; no duplicate parsers |
| Server read/write boundary: shared parser + fail-loud mappers, nullable `currentRole`, writes `.name` | ✅ Yes | Matches design boundary exactly |
| App local persistence: custom `ColumnAdapter` replacing `EnumColumnAdapter`, decode via shared parser, encode `.name` | ✅ Yes | Adapter lives in `composeApp` (app infrastructure), not `shared` |
| Legacy JSON request-body compatibility out of scope (no custom kotlinx serializer) | ✅ Yes | No serializer added; design `Resolved Scope Note` honored; spec does not require request-body compat |

## Documentation Coherence (Post-Review Reconciliation)

| Artifact | Status | Notes |
|----------|--------|-------|
| `archive-report.md` | ✅ Corrected | Test count updated to 65 (29 server + 36 compose); explicit note that direct Compose app local-cache compatibility coverage is now in tests; post-review reconciliation paragraph documents the added test. |
| `proposal.md` Rollback Plan | ✅ Strengthened | Replaces the prior single-paragraph rollback with a concrete 3-step migration-aware recovery path (compatibility hotfix → data migration/JWT rotation → complete revert). Coherent with the `archive-report.md` Rollback / Recovery Note. Both now warn that a plain revert is unsafe once `STUDENT` values have been emitted. |

## Issues Found

**CRITICAL**: None.

**WARNING**: None.

**SUGGESTION**:
1. **(Cosmetic, optional) Test method names still use "learner" in natural language** — `ServerIntegrationTest` methods `exercise completion uses authenticated learner identity and updates progress`, `exercise completion validates path body match and learner progress deprecation`, and `learner content hides correct answers`. These are not enum references and do not violate the success criterion (production source is clean of `UserRole.LEARNER`); the intentionally-named legacy compat test `legacy learner role values still authenticate and hydrate as student` is correctly descriptive of what it tests. Aligning the three non-legacy method names to "student" would match the updated spec terminology. Not required by the task list; non-blocking.

### Resolved Issues (from prior verification)

- ✅ **RESOLVED — Prior SUGGESTION 1 (App adapter legacy decode only indirectly covered).** The post-review follow-up added `KtorUserRepositoryTest > user role adapter decodes legacy learner rows and preserves canonical student rows`, which inserts a raw `"LEARNER"` string directly into the local SQLite DB via `driver.execute(...)` (bypassing the adapter's encode path), reads it back through the production `userRoleColumnAdapter.decode` via `selectUserById(...).executeAsOne()`, and asserts `UserRole.STUDENT`. It also verifies the canonical `STUDENT` round-trip (encode via `insertUser` + decode via `selectUserById`). The test passed at runtime (time=0.002s, 0 failures). The "local cache rows" compatibility requirement is now covered end-to-end through the production adapter. The prior PARTIAL compliance rating is upgraded to COMPLIANT.

## Verdict

**PASS**

All 23 tasks complete; all three verification commands succeed with 65 tests passing and zero failures (29 server + 36 compose app); no `UserRole.LEARNER` enum references remain in source; the shared parser, server persistence/JWT paths, and compose SQLDelight adapter all behave per design; both specs use student terminology with legacy `LEARNER` equivalence documented and JSON request-body compatibility explicitly out of scope. The prior SUGGESTION 1 (direct local-cache adapter coverage) is resolved by a passing end-to-end test through the production adapter. Archive-report and rollback documentation are corrected and coherent. One non-blocking cosmetic suggestion remains (test method natural-language terminology).

Ready for commit/push.
