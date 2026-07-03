# Verify Report: detalle-curso (Slice 2 — App Repository, DI & Session Hydration)

## Change
- **change_name**: `detalle-curso`
- **persistence mode**: hybrid (openspec file + engram)
- **verification scope**: Slice 2 only (`CourseRepository.enroll` wiring + session hydration + hydration tests + backend composition fix)
- **strict Tdd**: inactive

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total (Phase 2) | 4 |
| Tasks complete (Phase 2, in-slice) | 3 |
| Tasks incomplete (deferred to slice 3) | 1 (2.2 — `CourseDetailViewModel` DI registration, waits on slice-3 VM) |
| Phase 1 (already PASS from slice-1 report) | unchanged |
| Phase 3 tasks | not in scope |

### Completeness Table

| Slice-2 Task | Status | Evidence |
|---|---|---|
| 2.1 Enrollment in `CourseApi`, `KtorCourseRepository`, `MockCourseRepository` | DONE | `CourseApi.enroll()` POSTs `/courses/{id}/enroll`; `KtorCourseRepository.enroll()` calls API + `syncUserProgressToLocal()`; `MockCourseRepository.enroll()` returns synthetic `UserProgress` with `enrolledCourseIds` populated |
| 2.2 Register `CourseDetailViewModel` in `AppModule.kt` | DEFERRED | ViewModel does not exist yet (slice 3). DI registration must land alongside the VM. Not a slice-2 critical gap. |
| 2.3 Session hydration in `domain/auth/` | DONE | `KtorAuthRepository.hydrateSessionIfNeeded()` + `SessionHydrationResult` sealed interface added; `UserApi.fetchCurrentUser()` maps 401→`UnauthorizedSessionException`; `App.AuthGate` consumes hydration result with retry UI |
| 2.4 Hydration `commonTest` covering token-without-user / skip-already-hydrated / 401 / network error | DONE | `SessionHydrationTest` has 4 cases (all required scenarios covered) |

## Build & Tests Execution

**Build**: ✅ Passed
```text
./gradlew :composeApp:jvmTest --tests "com.example.proyectofinal.data.KtorCourseRepositoryTest" \
  --tests "com.example.proyectofinal.domain.auth.SessionHydrationTest" --rerun-tasks --console=plain
> Task :composeApp:jvmTest
BUILD SUCCESSFUL in 1m 59s
19 actionable tasks: 19 executed
```

**Tests**: ✅ pass
```text
TEST-com.example.proyectofinal.data.KtorCourseRepositoryTest.xml:
  tests="11" skipped="0" failures="0" errors="0"  (includes 2 new enroll tests)
TEST-com.example.proyectofinal.domain.auth.SessionHydrationTest.xml:
  tests="4"  skipped="0" failures="0" errors="0"
```

**Backend composition fix (slice-1 W1)**: ✅
```text
./gradlew :server:test --console=plain
> Task :server:test
BUILD SUCCESSFUL in 1m 3s
```
`server/Main.kt` now passes the shared `userService` into `CourseService(userService)` instead of letting it default-construct its own. This resolves slice-1 WARNING W1 safely and `:server:test` still passes.

**Coverage**: ➖ Not available for this module.

**Note on auth repository tests**: `KtorAuthRepositoryTest` was not filtered in the targeted rerun (existing fake `getCurrentUser` returns null), but `SessionHydrationTest` directly exercises `KtorAuthRepository.hydrateSessionIfNeeded()` with a `FakeHydrationUserRepository`, which is the authoritative hydration path.

## Spec Compliance Matrix

### course-enrollment — Client Enrollment Repository

| Requirement | Scenario | Test | Result |
|---|---|---|---|
| Client Enrollment Repository | Repository calls enrollment endpoint | `KtorCourseRepositoryTest > enroll posts to enrollment endpoint with bearer token and syncs progress locally` | ✅ COMPLIANT — asserts `Bearer token-123`, `POST`, `/courses/official-course/enroll` |
| Client Enrollment Repository | Repository returns updated progress | same test | ✅ COMPLIANT — asserts `assertEquals(expectedProgress, result)` + local DB sync (`selectEnrolledCoursesByUserId`, `selectProgressByUserId`) |
| Client Enrollment Repository | Repository propagates network errors | `KtorCourseRepositoryTest > enroll propagates remote failures` | ✅ COMPLIANT — `assertFailsWith<IllegalStateException>` with "Network unavailable" |

### course-enrollment — Enrollment CTA Replaces Visual-Only Button

| Requirement | Scenario | Test | Result |
|---|---|---|---|
| Enrollment CTA replaces visual button | Enroll button triggers network call | (none) | ➖ OUT OF SLICE 2 — UI catalog wiring is slice 3 |
| | Successful enrollment updates local state | (none) | ➖ OUT OF SLICE 2 — slice 3 |
| | Enrollment failure shows error | (none) | ➖ OUT OF SLICE 2 — slice 3 |

### session-hydration

| Requirement | Scenario | Test | Result |
|---|---|---|---|
| Session Restore Hydrates Current User | Valid token with missing user triggers hydration | `SessionHydrationTest > hydrateSessionIfNeeded fetches current user when token exists without user` | ✅ COMPLIANT |
| | Hydration succeeds and app enters authenticated flow | (no unit test; App.kt AuthGate routes to `resolveAuthView` once `sessionState.Ready` + `onboardingComplete` resolved) | ⚠️ PARTIAL — runtime hydration behavior covered; routing assertion deferred to slice 3 UI test |
| | Hydration fails with invalid token triggers re-login | `SessionHydrationTest > hydrateSessionIfNeeded clears invalid token after unauthorized response` | ✅ COMPLIANT — token cleared, `logout()` invoked; App.kt routing returns Login via unauthenticated branch |
| | Hydration fails with network error shows retry | `SessionHydrationTest > hydrateSessionIfNeeded preserves token and returns retryable failure on network error` | ✅ COMPLIANT — returns `Failed("Network unavailable")`, token retained; App.kt `AuthGateRestoreError` retry button wired (UI not unit-tested) |
| Hydration Must Complete Before Screen Data Fetches | Course detail waits for hydration | (none) | ➖ OUT OF SLICE 2 — detail screen is slice 3 |
| | Home dashboard waits for hydration | (none) | ➖ OUT OF SLICE 2 UI test — but `App.AuthGate` blocks `onboardingComplete`/`AuthenticatedHomeScaffold` on `sessionState.Ready`; SUGGESTION to cover in slice 3 |
| Hydration Is Idempotent | Already-hydrated session skips hydration | `SessionHydrationTest > hydrateSessionIfNeeded skips network call when session is already hydrated` | ✅ COMPLIANT — `currentUserCalls == 1`, result `Skipped` |

**Compliance summary (in-slice scenarios)**: 6/6 in-slice client/hydration scenarios COMPLIANT. Out-of-slice UI scenarios deferred to slice 3.

## Correctness (Static Evidence)

| Concern | Status | Notes |
|---|---|---|
| `enroll` honors official-only contract on client | ✅ | `MockCourseRepository.enroll()` filters `isOfficial`; `KtorCourseRepository` defers the 400/404/401 distinction to the backend (already covered by slice 1 server tests) |
| Bearer token attached to enroll request | ✅ | `createHttpClient` auth interceptor already injects the token; repo test asserts `Bearer token-123` |
| `enroll` syncs local persistence | ✅ | `syncUserProgressToLocal()` writes `progress`, `completedLesson(s)`, `enrolledCourse(s)` — mirrors existing `syncUser*ToLocal` patterns |
| Hydration distinguishable 401 vs network | ✅ | `UserApi.fetchCurrentUser()` maps only `Unauthorized` → `UnauthorizedSessionException`; hydrate catches it before the generic `Exception` branch so token is cleared only on 401, preserved on network/5xx |
| Hydration idempotent | ✅ | Early `return Skipped` when `currentSession.user != null` — test `currentUserCalls == 1` after 2 calls |
| `KtorUserRepository.getCurrentUser()` behavior change is safe | ✅ | Previously swallowed all exceptions and returned `null`; now propagates. Only production caller is `KtorAuthRepository.hydrateSessionIfNeeded()`, which handles all error categories explicitly. UI fakes unaffected. |
| `AuthSession` restoration seeds token-only state | ✅ | `KtorAuthRepository` constructor reads `tokenStore.accessToken` and emits `AuthSession(token = it)` (no user) — exactly the precondition hydration expects |
| DI graph coherent | ✅ (PARTIAL) | `KtorAuthRepository(get(), get(), get())` resolves `UserRepository` via existing single; `CourseDetailViewModel` registration still pending (slice 3) |
| Backend composition warning W1 resolved safely | ✅ | `server/Main.kt`: `CourseService(userService)`; `:server:test` passes |

## Coherence (Design)

| Design Decision | Followed? | Notes |
|---|---|---|
| `CourseRepository.enroll(courseId): UserProgress` contract | ✅ Yes | Matches design contract verbatim |
| Hydration reuses `UserRepository.getCurrentUser()` instead of a dedicated endpoint | ✅ Yes | `GET /users/{CurrentUserAlias}` aliased to "current-user-id" leverages existing JWT resolution on server; no new backend route needed in slice 2 |
| `SessionHydrationResult` sealed hierarchy distinguishes `Skipped`/`Hydrated`/`ClearedInvalidSession`/`Failed` | ✅ Yes | Maps cleanly to the three spec branches + skip idempotency |
| App.AuthGate is the single place observing hydration for navigation | ✅ Yes | `produceState` keyed on `session.token/user` + retry key drives Loading/Error/Ready; AuthenticatedHomeScaffold only renders when `Ready` |
| Enrollment endpoint layered through repository/api/double | ✅ Yes | `CourseApi.enroll` → `KtorCourseRepository.enroll` → `syncUserProgressToLocal`; `MockCourseRepository` also wired for tests/dev |

## Issues Found

### CRITICAL
- None.

### WARNING
- **W1 — Hydration "app enters authenticated flow / navigate to Login / show retry" routing is verified structurally, not at runtime.** The unit tests prove the `SessionHydrationResult` value and the `App.AuthGate` code reads cleanly, but no `commonTest` exercises the `AuthGate` Compose routing. SUGGESTION to add a Compose UI test (or a presenter-level seam) in slice 3 so the Ready/Error/Loading → screen mapping is asserted against the spec's navigation scenarios.
- **W2 — KtorUserRepository behavior change broadens the surface** of `getCurrentUser` from "never throws" to "throws on non-OK responses". Currently safe (only hydration calls it), but any future caller that treats `getCurrentUser()` as nullable will need to handle `UnauthorizedSessionException`/`IllegalStateException`. Document near `UserApi.fetchCurrentUser()` so future callers don't reintroduce the old silent-null pattern.

### SUGGESTION
- **S1 — Mock-course mockCourses uses Box-of-Lessons; `MockCourseRepository` enrollment isn't unit-tested.** It's a test double, but a tiny `MockCourseRepositoryTest` asserting `enroll` adds to `enrolledCourseIds` and respects `isOfficial` would lock the contract.
- **S2 — `Lesson.exerciseCount` default-on-missing-field client deserialization test (carried from slice-1 S1)** is still not added. The shared field has `= 0`, so behavior is structurally correct, but a one-line `commonTest` deserializing a `Lesson` JSON without `exerciseCount` would close the gap recommended in slice 1.
- **S3 — Reorder catches defensively**: in `hydrateSessionIfNeeded` the `UnauthorizedSessionException` catch precedes generic `Exception`. Correct as written; keep that ordering when refactoring — reordering silently changes invalid-token semantics.

## Verdict

**PASS**

All in-slice spec scenarios (3 client-enrollment + 4 in-slice session-hydration scenarios) have passing covering tests (`:composeApp:jvmTest` green, 11+4 tests passed). Backend composition fix from slice 1 (`CourseService(userService)` in `server/Main.kt`) verified via `:server:test` green. The deferred task 2.2 (`CourseDetailViewModel` DI registration) correctly waits for slice 3. UI-routing scenarios are tracked as WARNING W1 / S2 to be closed when slice 3 lands.

## Next Slice

Slice 3 scope (Phase 3): `ActivitiesTabRouter`, `CourseDetailScreen` + `CourseDetailViewModel`, catalog→detail navigation, enroll CTA wiring + local state update, UI `commonTest` covering detail VM derivation and catalog→detail nav, and DI registration task 2.2.