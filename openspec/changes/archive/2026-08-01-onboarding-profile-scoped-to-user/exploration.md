# Exploration: onboarding-profile-scoped-to-user

## Current State

`LearnerProfileEntity` (`AppDatabase.sq:65-71`) is a single global row (`profileId INTEGER NOT NULL PRIMARY KEY CHECK (profileId = 1)`), unlike every other per-user local table in the same file (`UserProgressEntity`, `CompletedLesson`, `CompletedExercise`, `EnrolledCourse` — all keyed by `userId`). A `clearProfile: DELETE FROM LearnerProfileEntity WHERE profileId = 1;` query already exists (from the original `2026-06-28-onboarding-school-year` change) but is **dead code** — zero references anywhere in Kotlin.

Decisive additional finding: `TokenStore`/`InMemoryTokenStore` (`di/TokenStore.kt`) is in-memory only — no persisted implementation exists on any platform. So **every cold app restart already routes back to LOGIN/REGISTER without ever calling `logout()`**. This means a "clear on logout" fix would not address the actual reported repro (restart → onboarding skipped), since `logout()` is not the path that was hit.

Server side has zero learner-profile/onboarding concept — this is a purely client-local bug, not a sync issue. No `.sqm` migration harness exists; local schema changes follow the ad-hoc `di/LocalDatabaseSchemaFixes.kt` `PRAGMA table_info` repair pattern (used for `CourseEntity`/`LessonEntity`/`ExerciseEntity`), which is directly reusable for an additive `ALTER TABLE ADD COLUMN userId`.

`AuthGate` (`App.kt:46-62`) calls `isOnboardingComplete()` with `session.user?.id` available but unused. `OnboardingViewModel` has no `AuthRepository` dependency today (would need one added); `ProfileViewModel`/`HomeDashboardViewModel`/`CourseViewModel` already inject `AuthRepository` alongside `LearnerProfileRepository`.

## Affected Areas

- `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq:65-71,153-163` — table + queries
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/data/SqlDelightLearnerProfileRepository.kt` — all methods unscoped
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/domain/LearnerProfileRepository.kt` — interface would need `userId` param
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/App.kt:46-62` — the actual bug-triggering read
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/OnboardingViewModel.kt` — needs `AuthRepository` injected
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixes.kt` — precedent for the ALTER TABLE fix
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/AppModule.kt` — Koin wiring
- `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/TokenStore.kt` — confirms no persisted session (context only)
- Tests: `SqlDelightLearnerProfileRepositoryTest`, `KtorAuthRepositoryTest`, `AuthGateViewModelTest`, `OnboardingViewModelTest`

## Approaches

1. **Add a `userId` column, scope `isOnboardingComplete`/`getProfile` reads by comparing to the currently authenticated user's id; `upsertProfile` writes current userId** — additive `ALTER TABLE ADD COLUMN` via existing repair pattern, no destructive migration.
   - Pros: Fixes the actual root cause; matches convention already used by 4 sibling tables; survives `TokenStore`'s in-memory nature because the marker is disk-persisted; bounded blast radius (gate-relevant methods only need scoping; downstream display-only consumers read post-gate so are already safe).
   - Cons: Touches ~5-6 files + tests; `OnboardingViewModel` needs a new `AuthRepository` dependency; devices with the pre-existing stale row get one more forced onboarding replay after migration (acceptable, not a new regression).
   - Effort: Medium

2. **Wire the dead `clearProfile` query into `AuthRepository.logout()`**
   - Pros: Minimal blast radius, reuses existing dead code, no schema change.
   - Cons: **Confirmed insufficient** — `TokenStore` has no persisted implementation, so the app already reaches LOGIN/REGISTER on every cold restart without calling `logout()`. This is almost certainly how the reporter re-triggered onboarding. Fixes only the narrower explicit-logout case, not the confirmed trigger.
   - Effort: Low, but does not close the reported gap

3. **Clear the profile row unconditionally on every successful `login()`/`register()`**
   - Pros: Simple, no schema change, fixes "new account never inherits old flag."
   - Cons: Forces onboarding to re-run on every cold start even for the same returning user (since sessions never persist), compounding the separately-known `TokenStore` non-persistence issue rather than fixing it.
   - Effort: Low, but net worse UX than approach 1

## Recommendation

Approach 1. It matches an existing, already-established schema convention (userId-keyed local tables), is purely additive (no destructive migration, reuses the `LocalDatabaseSchemaFixes.kt` precedent), and — critically — approach 2, which looked like the "proportionate, minimal" option, is demonstrably insufficient given the confirmed in-memory `TokenStore` behavior. Approach 3 fixes the symptom but introduces a more visible regression (onboarding re-running every restart). `sdd-propose` should decide whether userId-scoping extends only to the two gate-relevant methods (`isOnboardingComplete`, `getProfile`) or also to the downstream display consumers for defense-in-depth (cheap, since they already inject `AuthRepository`).

## Risks

- `OnboardingViewModel` needs a new `AuthRepository` constructor dependency (small DI/test-fixture change).
- Devices with the existing stale global row will see one additional forced onboarding replay post-migration for whichever account currently owns it — expected, should be named explicitly in the proposal/rollback plan.
- `TokenStore`'s lack of session persistence across restarts is a separate, already-documented pre-existing limitation (archived `2026-06-28-onboarding-school-year` exploration, Risk #4) that amplifies how often this bug surfaces; out of scope here but worth a one-line cross-reference so it isn't rediscovered as a surprise.
- No `.sqm` migration harness exists project-wide — must follow the ad-hoc `LocalDatabaseSchemaFixes.kt` pattern and its existing test conventions for consistency.

## Ready for Proposal

Yes.
