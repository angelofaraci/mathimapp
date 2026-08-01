# Design: Scope Learner Profile to the Authenticated User

## Technical Approach

Re-key `LearnerProfileEntity` from a single global row to one row per `userId`, matching the four sibling per-user tables in `AppDatabase.sq`. Every `LearnerProfileRepository` method takes `userId: String` (proposal Decision, settled). `AuthGate` feeds `session.user?.id` into the completion check and fails toward showing onboarding. Legacy databases are repaired at driver-open time by `LocalDatabaseSchemaFixes.kt`.

## Architecture Decisions

### Decision: `userId TEXT NOT NULL PRIMARY KEY`, `profileId` removed entirely

**Choice**: `userId` becomes the sole primary key; `profileId` and its `CHECK (profileId = 1)` are deleted. No separate index — the PK already covers every access path (all four queries filter on `userId` alone).
**Alternatives considered**: keep `profileId` as a surrogate PK plus a `UNIQUE(userId)` index.
**Rationale**: `UserProgressEntity` already uses bare `userId TEXT NOT NULL PRIMARY KEY`; a surrogate key buys nothing and leaves a second uniqueness rule to keep in sync.

### Decision: the schema repair is a table rebuild, not `ALTER TABLE ADD COLUMN`

**Choice**: `ensureLearnerProfileEntityShape()` rebuilds the table, following the existing `ensureLessonEntityShape()` precedent in the same file (`PRAGMA table_info` guard → `PRAGMA foreign_keys=OFF` → `BEGIN` → `CREATE ..._new` → `INSERT ... SELECT` → `DROP` → `RENAME` → `COMMIT`, with `ROLLBACK` on throw and `foreign_keys=ON` in `finally`). Legacy rows are carried over with `'' AS userId`.
**Alternatives considered**: the additive `ALTER TABLE LearnerProfileEntity ADD COLUMN userId TEXT NOT NULL DEFAULT ''` named in the proposal.
**Rationale**: **`ADD COLUMN` alone does not work here.** SQLite cannot drop a `CHECK`/PK by `ALTER`. An upgraded install would keep `profileId INTEGER NOT NULL PRIMARY KEY CHECK (profileId = 1)`; the new `upsertProfile` omits `profileId`, so SQLite auto-assigns rowid `2` for the second user and the `CHECK` aborts the insert. The bug would persist — worse, as a crash — on exactly the shared devices this change targets. The rebuild preserves the proposal's intent (purely local, no data wipe, no `.sqm` harness) and its `''` sentinel semantics: the sentinel matches no real user id, so the orphaned legacy row reads as "not onboarded" for everyone and onboarding replays once, as intended.

**Guard condition**: read `PRAGMA table_info(LearnerProfileEntity)`; return early if the map is empty (table not yet created) or if `userId` is present and `profileId` is absent (already repaired / fresh install). Otherwise rebuild. Register the call in `applyPendingLocalSchemaFixes()` after `ensureExerciseEntityShape()`.

### Decision: blank `userId` is rejected in two places

**Choice**: `AuthGate` treats `session.user?.id.isNullOrBlank()` as `onboardingComplete = false`; the repository independently returns `null` / `false` / no-op for a blank `userId`.
**Alternatives considered**: gate-only check; repository throwing on blank.
**Rationale**: fail-safe must be "show onboarding", never "skip it". Throwing would surface as a crash on a transient session gap; duplicating the cheap check keeps the invariant even if a future caller forgets it.

### Decision: Koin needs no `AppModule.kt` edit

**Choice**: leave `AppModule.kt` unchanged. `viewModelOf(::OnboardingViewModel)` and `viewModelOf(::CourseViewModel)` resolve the added `AuthRepository` constructor parameter reflectively, and `AuthRepository` is already a registered `single`.
**Alternatives considered**: switching to explicit `viewModel { OnboardingViewModel(get(), get()) }`.
**Rationale**: the module already relies on `viewModelOf` for eight ViewModels including `ProfileViewModel`, which takes `AuthRepository` the same way. Verification lives in `ComposeAppCommonTest`'s graph check, not in a wiring edit.

## Data Flow

    AuthRepository.session ──(user.id)──> AuthGate.produceState
                                              │
                                              ▼
                        LearnerProfileRepository.isOnboardingComplete(userId)
                                              │
                                    SELECT ... WHERE userId = ?
                                              │
              false ──> ONBOARDING ──> OnboardingViewModel.completeOnboarding()
                                              │
                        authRepository.session.value.user?.id ──> upsertProfile(userId, profile)
                                              │
                        onboardingRefreshKey += 1 ──> re-read ──> true ──> COURSE

Sequence, cross-account (the bug):

    User A: login → id="a" → isOnboardingComplete("a") = false → onboarding → upsert(userId="a")
    User B: login → id="b" → isOnboardingComplete("b") = false → onboarding  ← was previously true
    User A: login → id="a" → isOnboardingComplete("a") = true  → COURSE

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `composeApp/src/commonMain/sqldelight/com/example/proyectofinal/db/AppDatabase.sq` | Modify | Table re-keyed on `userId`; all four profile queries scoped |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/di/LocalDatabaseSchemaFixes.kt` | Modify | Add `ensureLearnerProfileEntityShape()` + registration |
| `.../domain/LearnerProfileRepository.kt` | Modify | `userId: String` first parameter on all three methods |
| `.../data/SqlDelightLearnerProfileRepository.kt` | Modify | Pass `userId` through; blank-id short circuit |
| `.../App.kt` | Modify | `AuthGate` reads and keys on `session.user?.id` |
| `.../ui/OnboardingViewModel.kt` | Modify | New `authRepository: AuthRepository` first constructor param |
| `.../ui/CourseViewModel.kt` | Modify | New `authRepository` param; scoped `getProfile` |
| `.../ui/ProfileViewModel.kt`, `.../ui/home/HomeDashboardViewModel.kt` | Modify | Pass existing session user id to `getProfile` |
| `.../di/AppModule.kt` | Unchanged | See decision above |
| `commonTest/.../{SqlDelightLearnerProfileRepositoryTest, OnboardingViewModelTest, ProfileViewModelTest, HomeDashboardViewModelTest, ComposeAppCommonTest}.kt` | Modify | Fake signatures + new cases |
| `jvmTest/.../LocalDatabaseSchemaFixesTest.kt` | Modify | New legacy-shape repair case |

## Interfaces / Contracts

`AppDatabase.sq`:

```sql
CREATE TABLE LearnerProfileEntity (
    userId TEXT NOT NULL PRIMARY KEY,
    province TEXT NOT NULL,
    schoolYear INTEGER NOT NULL,
    studentTrack TEXT NOT NULL,
    onboardingComplete INTEGER AS Boolean NOT NULL DEFAULT 0
);

selectProfile:
SELECT province, schoolYear, studentTrack, onboardingComplete
FROM LearnerProfileEntity WHERE userId = ?;

upsertProfile:
INSERT OR REPLACE INTO LearnerProfileEntity(userId, province, schoolYear, studentTrack, onboardingComplete)
VALUES (?, ?, ?, ?, ?);

clearProfile:
DELETE FROM LearnerProfileEntity WHERE userId = ?;
```

`isOnboardingComplete` stays derived in Kotlin from `selectProfile` (no dedicated SQL query today; do not add one).

```kotlin
interface LearnerProfileRepository {
    suspend fun getProfile(userId: String): LearnerProfile?
    suspend fun isOnboardingComplete(userId: String): Boolean
    suspend fun upsertProfile(userId: String, profile: LearnerProfile)
}
```

`userId` is `String` — it is `User.id` from `shared`, the same type used by `UserProgressEntity`, `CompletedLesson`, `CompletedExercise`, and `EnrolledCourse`. `LearnerProfile` itself gains no `userId` field: the id is the key, not profile data.

`OnboardingViewModel(authRepository: AuthRepository, learnerProfileRepository: LearnerProfileRepository)` — `completeOnboarding()` resolves `authRepository.session.value.user?.id`; if null/blank it sets `errorMessage` and returns without writing, leaving `isSaving = false`.

`AuthGate` uses the `produceState` vararg-keys overload with `session.user?.id` added as a key alongside `session.isAuthenticated`, `session.token`, and `onboardingRefreshKey`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit — repository | Two `userId`s hold independent profiles and independent `onboardingComplete`; writing A never flips B; blank `userId` returns `null` / `false`; `upsertProfile` replaces within one user only (row count stays 1 per user) | Extend `SqlDelightLearnerProfileRepositoryTest` using its existing `createTestAppDatabase()` + `createTestDriver()` fixture |
| Unit — migration | Legacy `profileId ... CHECK (profileId = 1)` table with one populated row survives `applyPendingLocalSchemaFixes()`: row still present with `userId = ''`, its province/schoolYear/track intact, `isOnboardingComplete("")` is `false`, and a subsequent two-user `upsertProfile` succeeds (proves the `CHECK` is gone) | New case in `LocalDatabaseSchemaFixesTest`, matching the raw-`JdbcSqliteDriver` + `CREATE TABLE` legacy-shape style of the three existing cases |
| Unit — ViewModel | `OnboardingViewModel.completeOnboarding()` calls `upsertProfile` with the session user id; with a null-user session it sets an error and does not write | Extend `OnboardingViewModelTest`; add an `AuthRepository` fake in the shape of `ProfileFakeAuthRepository` and give `FakeLearnerProfileRepository` a `Map<String, LearnerProfile>` store |
| Unit — routing | `resolveAuthView` with `onboardingComplete = false` yields `ONBOARDING` for an authenticated session (already covered) | `AuthGateRoutingTest` — verify existing cases still assert the fail-safe direction; extend only if a gap appears |
| Integration | Koin graph still constructs `OnboardingViewModel` and `CourseViewModel` after the new dependency | Existing `ComposeAppCommonTest` module check; update its `FakeLearnerProfileRepository` |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. The `AuthGate` "routing" here is in-process Compose view selection, not request routing.

## Migration / Rollout

One-shot, at driver-open time, before `AppDatabase` is constructed (`AppModule.createAppDatabase` already chains `.applyPendingLocalSchemaFixes()`). Idempotent via the `PRAGMA table_info` guard. Fresh installs get the new shape from `.sq` and skip the repair. Existing installs keep their row under `userId = ''` and replay onboarding once, per the proposal. Rollback is a single revert; the rebuilt table is readable by the reverted `SELECT`/`INSERT` only if `profileId` is restored, so a revert must also revert the `.sq` file — noted below as a risk refinement to the proposal's rollback plan.

## Open Questions

- [ ] None blocking. The `ADD COLUMN` → table-rebuild change is a mechanism correction within the proposal's stated approach, not a scope change; flagged for orchestrator acknowledgement.
